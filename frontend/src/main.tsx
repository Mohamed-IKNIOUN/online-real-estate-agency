import {favouritesEnabled,visitsEnabled} from './features'
import {StrictMode,useRef,useState} from 'react'
import {createRoot} from 'react-dom/client'
import {BrowserRouter,Link,NavLink,Outlet,Route,Routes,useNavigate} from 'react-router-dom'
import {Activity,Building2,LogOut,Menu,RefreshCw,X} from 'lucide-react'
import {ApiError,request,type Service} from './api'
import {AuthProvider,Guard,homeFor,useAuth} from './auth'
import {Detail,Home,Listings,Login,Problem} from './public'
import {Account,Favourites,Offers,Overview,Properties,Visits,Workspace} from './workspace'
import {Empty,Feedback,Heading,Query,useAction,useQuery} from './ui'
import './styles.css'

function Layout(){
  const [open,setOpen]=useState(false)
  const menuButton=useRef<HTMLButtonElement>(null)
  const {session,logout}=useAuth()
  const action=useAction()
  const navigate=useNavigate()
  return <div className="site"><a className="button primary skip-link" href="#contenu">Aller au contenu</a><header className="site-header"><div className="wrap header-inner">
  <Link to="/" className="brand" onClick={()=>setOpen(false)}><span className="brand-icon"><Building2 size={23}/></span><span>AQAR<small>IMMOBILIER</small></span></Link>
  <button ref={menuButton} type="button" className="icon-button menu-toggle" aria-label={open?'Fermer le menu':'Ouvrir le menu'} aria-expanded={open} aria-controls="site-nav" onClick={()=>setOpen(!open)}>{open?<X/>:<Menu/>}</button>
  <nav className={open?'open':''} id="site-nav" aria-label="Navigation principale" onKeyDown={event=>{if(open&&event.key==="Escape"){setOpen(false);menuButton.current?.focus()}}} onClick={()=>setOpen(false)}><NavLink end to="/">Accueil</NavLink><NavLink to="/biens">Les biens</NavLink><NavLink to="/services"><Activity size={16}/> Services</NavLink>{session?<><NavLink to={homeFor(session.user.role)}>Mon espace</NavLink><button className="button outline" onClick={()=>{navigate('/connexion',{replace:true});void action.run(logout,'')}}><LogOut size={16} aria-hidden="true"/> Déconnexion</button></>:<><NavLink to="/connexion">Connexion</NavLink><Link className="button primary" to="/inscription">Créer un compte</Link></>}</nav></div></header>
  {action.error&&<div className="wrap"><Feedback error={'Session locale fermée. '+action.error}/></div>}
  <div className="site-body" id="contenu" tabIndex={-1}><Outlet/></div><footer><div className="wrap footer-inner"><strong>AQAR</strong><Link to="/services">État des services locaux</Link></div></footer></div>
}
const services:{id:Service; label:string; key:string; route:string}[]=[
  {id:'identity',label:'Identité',key:'AQAR_PROXY_IDENTITY_URL',route:'/api/users/me'},
  {id:'announcements',label:'Annonces',key:'AQAR_PROXY_ANNOUNCEMENTS_URL',route:'/api/announcements'},
  {id:'favourites',label:'Favoris',key:'AQAR_PROXY_FAVOURITES_URL',route:'/api/favoris/client/1'},
  {id:'visits',label:'Visites',key:'AQAR_PROXY_VISITS_URL',route:'/api/visits'},
  {id:'offers',label:'Offres',key:'AQAR_PROXY_OFFERS_URL',route:'/api/offers'}
]
function Unavailable({name}:{name:string}){
  return <Empty title={name+' indisponibles'} copy="Ce parcours exige une version du backend qui contrôle les autorisations utilisateur."/>
}
function Services(){
  const load=()=>Promise.all(services.filter(service=>(service.id!=="visits"||visitsEnabled)&&(service.id!=="favourites"||favouritesEnabled)).map(async service=>{
    try {await request(service.id,service.route);return {...service,state:'Accessible',detail:'La route de vérification répond.',ok:true}}
    catch(err){
      if(err instanceof ApiError && [401,403].includes(err.status))return {...service,state:'Accessible · protégé',detail:'HTTP '+err.status+' sans jeton.',ok:true}
      return {...service,state:'Vérification échouée',detail:err instanceof Error?err.message:'Erreur réseau inconnue.',ok:false}
    }
  }))
  const status=useQuery(load,'services')
  return <main className="wrap section stack"><Heading title="Connexions aux services" copy="Vérifications GET locales, sans jeton ni modification des données." action={<button className="button outline" disabled={status.loading} aria-busy={status.loading} onClick={status.reload}>{!status.loading&&<RefreshCw size={16} aria-hidden="true"/>}{status.loading?"Actualisation…":"Actualiser"}</button>}/>
  <Query state={status}>{items=><div className="cards">{items.map(item=><article className="panel stack" key={item.id}><h2>{item.label}</h2><span className={'badge '+(item.ok?'':'badge-error')}>{item.state}</span><code>{item.key}</code><p>{item.detail}</p></article>)}</div>}</Query>
  </main>
}
export function Application(){
  return <AuthProvider><Routes><Route element={<Layout/>}>
    <Route index element={<Home/>}/><Route path="biens" element={<Listings/>}/><Route path="biens/:id" element={<Detail/>}/>
    <Route path="connexion" element={<Login key="login"/>}/><Route path="inscription" element={<Login key="registration" register/>}/>
    <Route path="services" element={<Services/>}/><Route path="acces-refuse" element={<Problem forbidden/>}/>
    {(['BUYER','AGENT','ADMIN'] as const).map(role=><Route key={role} element={<Guard role={role}/>}>
      <Route path={homeFor(role)} element={<Workspace/>}>
        <Route index element={<Overview role={role}/>}/>
        {role==='BUYER'?<Route path="favoris" element={favouritesEnabled?<Favourites/>:<Unavailable name="Favoris"/>}/>:<Route path="annonces" element={<Properties role={role}/>}/>}
        <Route path="visites" element={visitsEnabled?<Visits role={role}/>:<Unavailable name="Visites"/>}/><Route path="offres" element={<Offers role={role}/>}/>
        <Route path="compte" element={<Account/>}/>
      </Route></Route>)}
    <Route path="*" element={<Problem/>}/>
  </Route></Routes></AuthProvider>
}
createRoot(document.getElementById('root')!).render(<StrictMode><BrowserRouter><Application/></BrowserRouter></StrictMode>)
