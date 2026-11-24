import {createContext,useContext,useEffect,useRef,useState,type ReactNode} from 'react'
import {Link,Navigate,Outlet,useLocation} from 'react-router-dom'
import {LoaderCircle} from 'lucide-react'
import {api,ApiError,failure,type Role,type User} from './api'
import {Feedback} from './ui'

type Session={token:string; refresh?:string; user:User; subject:string; expires:number}
type Restoration={status:'loading'|'complete'}|{status:'error'; message:string}
type AuthState={
  session:Session|null
  restoration:Restoration
  retry:()=>void
  login:(email:string,password:string)=>Promise<Session>
  logout:()=>Promise<void>
}
const Auth=createContext<AuthState|null>(null)
const accessKey='maison-adresse.access-token'

export const homeFor=(role:Role)=>role==='BUYER'?'/acheteur':role==='AGENT'?'/agent':'/admin'
export const roleName={BUYER:'Acheteur',AGENT:'Agent',ADMIN:'Administrateur'}

class InvalidSession extends Error {}

function readAccessToken(){
  if(!import.meta.env.DEV)return null
  try {return window.sessionStorage.getItem(accessKey)}
  catch {return null}
}
function writeAccessToken(token:string|null){
  try {
    if(token===null) window.sessionStorage.removeItem(accessKey)
    else if(import.meta.env.DEV) window.sessionStorage.setItem(accessKey,token)
  } catch {
    // Authentication remains usable in memory if browser storage is blocked.
  }
}
function claims(token:string){
  try {
    if(!/^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/.test(token)) throw new Error()
    const part=token.split('.')[1]
    const value=JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(part.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0))))
    if(typeof value.sub!=='string'||!/^[1-9][0-9]*$/.test(value.sub)||
       !['BUYER','AGENT','ADMIN'].includes(value.role)||!Number.isFinite(value.exp)||
       value.exp*1000<=Date.now()) throw new Error()
    return value as {sub:string;role:Role;exp:number}
  } catch {
    throw new InvalidSession('Le jeton est invalide ou expiré.')
  }
}
async function validatedSession(token:string):Promise<Session>{
  const decoded=claims(token)
  const user=await api.me(token)
  if(!user||String(user.id)!==decoded.sub||user.role!==decoded.role||decoded.exp*1000<=Date.now()){
    throw new InvalidSession('Le profil retourné ne correspond pas à une session valide.')
  }
  return {token,user,subject:decoded.sub,expires:decoded.exp*1000}
}
const isInvalidSession=(error:unknown)=>error instanceof InvalidSession||
  (error instanceof ApiError&&[401,403,404].includes(error.status))

export function AuthProvider({children}:{children:ReactNode}){
  const [session,setSession]=useState<Session|null>(null)
  const [restoration,setRestoration]=useState<Restoration>({status:'loading'})
  const [attempt,setAttempt]=useState(0)
  const generation=useRef(0)

  useEffect(()=>{
    let active=true
    const revision=generation.current
    const token=readAccessToken()
    if(!token){
      setRestoration({status:'complete'})
      return
    }

    void validatedSession(token).then(next=>{
      if(!active||generation.current!==revision)return
      setSession(next)
      setRestoration({status:'complete'})
    }).catch(error=>{
      if(!active||generation.current!==revision)return
      if(isInvalidSession(error)){
        writeAccessToken(null)
        setRestoration({status:'complete'})
      }else{
        setRestoration({status:'error',message:failure(error)})
      }
    })
    return ()=>{active=false}
  },[attempt])

  function clear(){
    generation.current++
    writeAccessToken(null)
    setSession(null)
    setRestoration({status:'complete'})
  }

  useEffect(()=>{
    if(!session)return
    const revision=generation.current
    const timer=window.setTimeout(()=>{
      if(generation.current===revision)clear()
    },Math.max(0,session.expires-Date.now()))
    return ()=>window.clearTimeout(timer)
  },[session])

  const retry=()=>{
    setRestoration({status:'loading'})
    setAttempt(previous=>previous+1)
  }
  const login=async(email:string,password:string)=>{
    const result=await api.login(email,password)
    const validated=await validatedSession(result.accessToken)
    if(validated.user.id!==result.id||validated.user.role!==result.role){
      throw new InvalidSession('L’identité du compte ne correspond pas au jeton reçu.')
    }
    const next={...validated,refresh:result.refreshToken}
    generation.current++
    writeAccessToken(next.token)
    setSession(next)
    setRestoration({status:'complete'})
    return next
  }
  const logout=async()=>{
    const refresh=session?.refresh
    clear()
    if(refresh) await api.logout(refresh)
  }

  return <Auth.Provider value={{session,restoration,retry,login,logout}}>{children}</Auth.Provider>
}

export function useAuth(){
  const auth=useContext(Auth)
  if(!auth) throw new Error('Contexte de connexion manquant.')
  return auth
}

export function Guard({role}:{role:Role}){
  const {session,restoration,retry}=useAuth()
  const location=useLocation()
  const state={next:location.pathname+location.search}
  if(restoration.status==='loading') return <div className="wrap section"><div className="loading" role="status"><LoaderCircle className="animate-spin" size={23}/>Restauration de la session…</div></div>
  if(restoration.status==='error') return <div className="wrap section stack"><Feedback error={restoration.message}/><button className="button outline" onClick={retry}>Réessayer la restauration</button><Link to="/connexion" state={state}>Se reconnecter</Link></div>
  if(!session) return <Navigate to="/connexion" replace state={state}/>
  return session.user.role===role?<Outlet/>:<Navigate to="/acces-refuse" replace/>
}
