import {LocalImage} from './imageViews'
import {useImages} from './propertyImages'
import {useCallback,useEffect,useState,type ReactNode,type FormEvent} from 'react'
import {Link} from 'react-router-dom'
import {AlertCircle,CheckCircle2,MapPin,Maximize,RefreshCw,SearchX} from 'lucide-react'
import {currency,failure,kinds,transactions,type Property} from './api'

export function useQuery<T>(load:()=>Promise<T>,key:string){
  const [state,set]=useState<{key:string; data?:T; error?:string; loading:boolean}>({key,loading:true})
  const [version,bump]=useState(0)
  const reload=useCallback(()=>bump(v=>v+1),[])
  useEffect(()=>{
    let live=true
    set({key,loading:true})
    load().then(data=>{if(live)set({key,data,loading:false})}).catch(err=>{if(live)set({key,error:failure(err),loading:false})})
    return()=>{live=false}
    // The key identifies the request and its account or filters.
  },[key,version])
  const update=(apply:(data:T)=>T)=>set(state=>state.key===key&&state.data!==undefined?{...state,data:apply(state.data)}:state)
  return {...(state.key===key?state:{loading:true,data:undefined,error:undefined}),reload,update}
}
export function useAction(){
  const [busy,setBusy]=useState(false),[error,setError]=useState(''),[notice,setNotice]=useState('')
  const run=async(task:()=>Promise<unknown>,message:string,done?:()=>void)=>{
    setBusy(true);setError('');setNotice('')
    try {await task();setNotice(message);done?.()} catch(err){setError(failure(err))} finally {setBusy(false)}
  }
  return {run,busy,error,notice}
}
export function Feedback({error,notice}:{error?:string;notice?:string}){
  return error?<div className="feedback error" role="alert"><AlertCircle size={19}/>{error}</div>:notice?<div className="feedback success" role="status"><CheckCircle2 size={19}/>{notice}</div>:null
}
export function Query<T>({state,children}:{state:{data?:T;error?:string;loading:boolean;reload:()=>void};children:(data:T)=>ReactNode}){
  if(state.loading)return <div className="loading" role="status"><span className="loading-skeleton" aria-hidden="true"/><span className="loading-skeleton short" aria-hidden="true"/><span>Chargement…</span></div>
  if(state.error)return <div className="stack"><Feedback error={state.error}/><button className="button outline" onClick={state.reload}><RefreshCw size={16} aria-hidden="true"/> Réessayer</button></div>
  return state.data!==undefined?children(state.data):null
}
export function Empty({title='Aucun résultat',copy,children}:{title?:string;copy:string;children?:ReactNode}){
  return <div className="empty"><SearchX size={30}/><h3>{title}</h3><p>{copy}</p>{children}</div>
}
export function Heading({title,copy,action,level=1}:{title:string;copy?:string;action?:ReactNode;level?:1|2}){
  const Title=level===2?"h2":"h1"
  return <div className="heading"><div><Title>{title}</Title>{copy&&<p>{copy}</p>}</div>{action}</div>
}
export function Card({item,action}:{item:Property;action?:ReactNode}){
  const {photos}=useImages(item)
  return <article className="property-card"><Link to={'/biens/'+item.id} className="property-image" aria-label={'Voir '+item.title}><LocalImage blob={photos[0]} alt=""/><span>{transactions[item.transactionType]}</span></Link><div className="property-info"><div className="meta"><MapPin size={15}/>{item.city}</div><h3><Link to={'/biens/'+item.id}>{item.title}</Link></h3><div className="meta"><Maximize size={14}/>{item.area} m² <span>· {kinds[item.propertyType]}</span></div><div className="card-bottom"><strong>{currency(item.price)}</strong>{action}</div></div></article>
}
export function StatusBadge({status,children}:{status:string;children:ReactNode}){
  const variant=['ACCEPTED','CONFIRMEE'].includes(status)?'success'
    :['REJECTED','REFUSEE'].includes(status)?'error'
    :['PENDING','DEMANDEE'].includes(status)?'pending':'muted'
  return <span className={'badge badge-'+variant}>{children}</span>
}
export function table(headers:string[],rows:ReactNode[][]){
  return <div className="table-shell"><table role="table"><thead role="rowgroup"><tr role="row">{headers.map(h=><th role="columnheader" scope="col" key={h}>{h}</th>)}</tr></thead><tbody role="rowgroup">{rows.map((cells,i)=><tr role="row" key={i}>{cells.map((cell,j)=><td role="cell" data-label={headers[j]} key={j}><div>{cell}</div></td>)}</tr>)}</tbody></table></div>
}
export const formData=(event:FormEvent<HTMLFormElement>)=>{event.preventDefault();return new FormData(event.currentTarget)}
export const value=(data:FormData,key:string)=>String(data.get(key)??'').trim()
