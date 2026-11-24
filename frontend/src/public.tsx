import {favouritesEnabled,visitsEnabled} from './features'
import {PropertyGallery} from './imageViews'
import {useState} from 'react'
import {Link,useLocation,useNavigate,useParams,useSearchParams} from 'react-router-dom'
import {Heart,MapPin,Search,SlidersHorizontal} from 'lucide-react'
import {api,currency,failure,kinds,transactions,type Property} from './api'
import {homeFor,useAuth} from './auth'
import {Card,Empty,Feedback,formData,Heading,Query,useAction,useQuery,value} from './ui'

export function Filters({hero=false}:{hero?:boolean}){
  const [params]=useSearchParams()
  const navigate=useNavigate()
  const fields=hero?['q','transactionType','propertyType']:['q','transactionType','propertyType','min','max']
  return <form className={'filters '+(hero?'hero-filters':'')} key={params.toString()} onSubmit={event=>{
    const data=formData(event)
    const query=new URLSearchParams()
    for(const key of fields){const field=value(data,key);if(field) query.set(key,field)}
    navigate('/biens'+(query.size?'?'+query:''))
  }}>
    <label>Ville ou mots-clés<input name="q" defaultValue={params.get('q')??''} placeholder="Ex. Casablanca"/></label>
    <label>Transaction<select name="transactionType" defaultValue={params.get('transactionType')??''}><option value="">Toutes</option>{Object.entries(transactions).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
    <label>Type de bien<select name="propertyType" defaultValue={params.get('propertyType')??''}><option value="">Tous les biens</option>{Object.entries(kinds).map(([key,label])=><option key={key} value={key}>{label}</option>)}</select></label>
    {!hero&&<><label>Prix minimum<input type="number" name="min" min="0" defaultValue={params.get('min')??''}/></label><label>Prix maximum<input type="number" name="max" min="0" defaultValue={params.get('max')??''}/></label></>}
    <button className="button primary"><Search size={18}/> Rechercher</button>
  </form>
}
export function Home(){
  const list=useQuery(api.properties,'public-properties')
  return <><section className="hero"><div className="wrap hero-content"><span className="eyebrow">AQAR IMMOBILIER</span><h1>Trouvez le lieu<br/>qui vous <em>ressemble.</em></h1><p>Explorez les biens disponibles à la vente et à la location.</p></div></section>
    <div className="wrap search-bridge"><Filters hero/></div>
    <section className="wrap section"><Heading level={2} title="Les biens à découvrir" copy="Parcourez les annonces disponibles." action={<Link className="button outline" to="/biens">Toutes les annonces</Link>}/>
    <Query state={list}>{items=>items.length?<div className="cards">{items.slice(0,3).map(item=><Card key={item.id} item={item}/>)}</div>:<Empty title="Aucune annonce publiée" copy="Les biens s’afficheront ici dès leur publication."/ >}</Query></section></>
}
export function Listings(){
  const [params]=useSearchParams()
  const list=useQuery(api.properties,'public-properties')
  const term=(params.get('q')??'').toLocaleLowerCase('fr')
  const filtered=(item:Property)=>[item.title,item.city,item.description].some(text=>text?.toLocaleLowerCase('fr').includes(term))
    && (!params.get('propertyType')||item.propertyType===params.get('propertyType'))
    && (!params.get('transactionType')||item.transactionType===params.get('transactionType'))
    && (!params.get('min')||item.price>=Number(params.get('min')))
    && (!params.get('max')||item.price<=Number(params.get('max')))
  return <div className="wrap section stack"><Heading title="Les annonces" copy="Affinez votre recherche par ville, type, transaction et prix."/><div className="panel stack"><span className="meta"><SlidersHorizontal size={16}/> Critères de recherche</span><Filters/>{params.size>0&&<Link to="/biens">Effacer les filtres</Link>}</div>
    <Query state={list}>{items=>{const visible=items.filter(filtered);return <><p className="muted">{visible.length} {visible.length===1?'annonce':'annonces'}</p>{visible.length?<div className="cards">{visible.map(item=><Card key={item.id} item={item}/>)}</div>:<Empty copy={items.length?'Aucun bien ne correspond à ces critères.':'Le catalogue est actuellement vide.'}/>}</>}}</Query></div>
}
export function Detail(){
  const id=Number(useParams().id)
  const {session}=useAuth()
  const buyer=session?.user.role==='BUYER'?session:null
  const item=useQuery(()=>Number.isSafeInteger(id)&&id>0?api.property(id):Promise.reject(new Error('Identifiant de bien invalide.')),'property:'+id)
  const favourites=useQuery(()=>buyer&&favouritesEnabled?api.favourites(buyer.subject,buyer.token):Promise.resolve([]),'favourites:'+(buyer?.subject??'anonymous'))
  const action=useAction()
  const saved=!!favourites.data?.find(fav=>fav.annonceId===id)
  return <div className="wrap section stack"><Link to="/biens">Retour aux annonces</Link><Query state={item}>{property=><><Heading title={property.title} copy={transactions[property.transactionType]}/><div className="detail-grid"><PropertyGallery key={property.id} property={property}/><div className="panel stack"><div className="meta"><MapPin size={18}/>{property.city}</div><h2>{currency(property.price)}</h2><p>{kinds[property.propertyType]} · {property.area} m²</p>
    {buyer?<>{favouritesEnabled&&<><Feedback error={action.error||favourites.error} notice={action.notice}/>{favourites.error&&<button className="button outline" onClick={favourites.reload}>Réessayer les favoris</button>}<button className="button outline" disabled={action.busy||favourites.loading||!!favourites.error} onClick={()=>action.run(()=>api.setFavourite(buyer.subject,id,buyer.token,saved),saved?'Favori retiré.':'Bien ajouté à vos favoris.',favourites.reload)}><Heart size={17}/>{saved?'Retirer des favoris':'Ajouter aux favoris'}</button></>}{visitsEnabled&&<Link className="button primary" to={'/acheteur/visites?bien='+id}>Réserver une visite</Link>}<Link className="button outline" to={'/acheteur/offres?bien='+id}>Faire une offre</Link></>:!session&&<Link className="button primary" to="/connexion" state={{next:'/biens/'+id}}>Se connecter pour agir</Link>}
    </div></div><section className="panel"><h2>Description</h2><p className="preserve">{property.description||'Aucune description fournie.'}</p></section></>}</Query></div>
}
export function Login({register=false}:{register?:boolean}){
  const auth=useAuth()
  const action=useAction()
  const navigate=useNavigate()
  const location=useLocation()
  const [registered,setRegistered]=useState(false)
  return <section className="wrap section auth-wrap"><form className="panel stack" onSubmit={event=>{
    const data=formData(event)
    const email=value(data,'email'), password=String(data.get('password')??'')
    if(register){
      return action.run(()=>api.register({email,password,fullName:value(data,'fullName'),role:value(data,'role')==='AGENT'?'AGENT':'BUYER'}),'Compte créé. Vous pouvez maintenant vous connecter.',()=>setRegistered(true))
    }
    return action.run(async()=>{const session=await auth.login(email,password);const next=location.state?.next; const destination=homeFor(session.user.role); const route=typeof next==='string'?next.split('?')[0]:''; const allowed=route===destination||route.startsWith(destination+'/')||route==='/biens'||route.startsWith('/biens/'); navigate(allowed?next:destination,{replace:true})},'Connexion réussie.')
  }}><Heading title={register?'Créer un compte':'Connexion'} copy={register?'Choisissez un compte acheteur ou agent.':'Accédez à votre espace immobilier.'}/><Feedback error={action.error} notice={action.notice}/>
  {registered?<Link className="button primary" to="/connexion">Aller à la connexion</Link>:<>
  {register&&<label>Nom complet<input name="fullName" required maxLength={150} autoComplete="name"/></label>}
  <label>Adresse e-mail<input name="email" type="email" required maxLength={150} autoComplete="email"/></label>
  <label>Mot de passe<input name="password" type="password" required minLength={register?8:undefined} maxLength={72} autoComplete={register?'new-password':'current-password'}/></label>
  {register&&<label>Type de compte<select name="role"><option value="BUYER">Acheteur</option><option value="AGENT">Agent</option></select></label>}
  <button className="button primary" disabled={action.busy} aria-busy={action.busy}>{action.busy?'Envoi…':register?'Créer mon compte':'Me connecter'}</button>
  <Link to={register?'/connexion':'/inscription'}>{register?'J’ai déjà un compte':'Créer un compte'}</Link></>}</form></section>
}
export function Problem({forbidden=false}:{forbidden?:boolean}){
  return <div className="wrap section"><Empty title={forbidden?'Accès refusé':'Page introuvable'} copy={forbidden?'Votre rôle ne permet pas d’accéder à cet espace.':'Cette adresse ne correspond à aucune page.'}><Link className="button primary" to="/">Retour à l’accueil</Link></Empty></div>
}
export {failure}
