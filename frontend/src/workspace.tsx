import {favouritesEnabled,visitsEnabled,offerDecisionsEnabled} from './features'
import {ImageEditor} from './imageViews'
import {imageError,removeImages,saveImages,useImages} from './propertyImages'
import {useEffect,useId,useRef,useState,type ReactNode} from 'react'
import {Link,NavLink,Outlet,useSearchParams} from 'react-router-dom'
import {Building2,CalendarDays,Check,ClipboardList,Heart,LayoutDashboard,Pencil,Plus,Trash2,UserRound,X} from 'lucide-react'
import {api,currency,failure,kinds,transactions,visitStatus,type Offer,type Property,type PropertyInput,type Role,type Visit} from './api'
import {homeFor,roleName,useAuth} from './auth'
import {Card,Empty,Feedback,formData,Heading,Query,StatusBadge,table,useAction,useQuery,value} from './ui'

export function Workspace(){
  const {session}=useAuth()
  if(!session)return null
  const role=session.user.role
  const root=homeFor(role)
  return <div className="wrap workspace">
    <aside className="sidebar"><div className="side-title"><span className="eyebrow">ESPACE {roleName[role].toUpperCase()}</span><strong>{session.user.fullName}</strong></div>
    <nav aria-label="Espace personnel"><NavLink end to={root}><LayoutDashboard/> Vue d’ensemble</NavLink>
    {role==='BUYER'?(favouritesEnabled&&<NavLink to={root+'/favoris'}><Heart/> Mes favoris</NavLink>):<NavLink to={root+'/annonces'}><Building2/> Annonces</NavLink>}
    {visitsEnabled&&<NavLink to={root+'/visites'}><CalendarDays/> {role==='BUYER'?'Mes visites':'Visites'}</NavLink>}<NavLink to={root+'/offres'}><ClipboardList/> {role==='BUYER'?'Mes offres':'Offres'}</NavLink>
    <NavLink to={root+'/compte'}><UserRound/> Mon compte</NavLink></nav></aside>
    <main className="workspace-content stack"><Outlet/></main>
  </div>
}
export function Overview({role}:{role:Role}){
  const {session}=useAuth()
  const root=homeFor(role), id=session!.subject, token=session!.token
  const main=useQuery(()=>role==='BUYER'?(favouritesEnabled?api.favourites(id,token).then(r=>r.length):Promise.resolve(0)):api.properties().then(r=>r.filter(p=>role==='ADMIN'||String(p.agentId)===id).length),'overview-primary:'+id+role)
  const visits=useQuery(()=>visitsEnabled?api.visits(token,role==='ADMIN'?undefined:role==='BUYER'?'client':'agent',id):Promise.resolve([]),'overview-visits:'+id+role)
  const offers=useQuery(()=>role==='AGENT'?agentOffers(token,id):api.offers(token,role==='BUYER'?'buyer':undefined,id),'overview-offers:'+id+role)
  const labels=[['Biens favoris','favoris'],['Visites','visites'],['Offres','offres']]
  if(role!=='BUYER') labels[0]=['Annonces','annonces']
  return <><Heading title="Vue d’ensemble" copy={'Bonjour '+session!.user.fullName+'.'}/>
  <div className="metric-grid">{[main,{...visits,data:visits.data?.length},{...offers,data:offers.data?.length}].map((state,i)=>(i===0&&role==='BUYER'&&!favouritesEnabled||i===1&&!visitsEnabled)?null:<section className="panel stack" key={labels[i][1]}><Link to={root+'/'+labels[i][1]}>{labels[i][0]}</Link><Query state={state}>{num=><strong className="metric">{num}</strong>}</Query></section>)}</div>
  {role==='ADMIN'&&<div className="panel"><h2>Fonctions administratives disponibles</h2><p>Gestion des annonces et consultation des offres. Les actions disponibles dépendent des services déployés. Le compte administrateur doit déjà exister.</p></div>}</>
}
export function Account(){
  const {session}=useAuth()
  const profile=useQuery(()=>api.me(session!.token),'account:'+session!.subject)
  return <><Heading title="Mon compte" copy="Informations renvoyées par le service d’identité."/><Query state={profile}>{user=><dl className="panel facts"><dt>Nom</dt><dd>{user.fullName}</dd><dt>E-mail</dt><dd>{user.email}</dd><dt>Rôle</dt><dd>{roleName[user.role]}</dd><dt>Identifiant</dt><dd>{user.id}</dd></dl>}</Query><p className="muted">L’API ne fournit pas de modification du profil.</p></>
}
export function Favourites(){
  const {session}=useAuth()
  const favs=useQuery(()=>api.favourites(session!.subject,session!.token),'buyer-favourites:'+session!.subject)
  const list=useQuery(api.properties,'properties')
  const action=useAction()
  return <><Heading title="Mes favoris" action={<Link className="button outline" to="/biens">Explorer les annonces</Link>}/><Feedback error={action.error} notice={action.notice}/><Query state={favs}>{items=>items.length?<><Feedback error={list.error}/><div className="cards">{items.map(f=>{
    const p=list.data?.find(candidate=>candidate.id===f.annonceId)
    const remove=<button className="button outline" disabled={action.busy} onClick={()=>action.run(()=>api.setFavourite(session!.subject,f.annonceId,session!.token,true),'Favori retiré.',favs.reload)}>Retirer</button>
    return p?<Card key={f.id} item={p} action={remove}/>:<article className="panel stack" key={f.id}><h3><Link to={'/biens/'+f.annonceId}>Bien nº {f.annonceId}</Link></h3>{remove}</article>
  })}</div></>:<Empty title="Aucun favori" copy="Ajoutez un bien depuis sa fiche."/ >}</Query></>
}

function Modal({title,close,children}:{title:string;close:()=>void;children:ReactNode}){
  const ref=useRef<HTMLDialogElement>(null)
  useEffect(()=>{
    const dialog=ref.current
    if(!dialog)return
    const trigger=document.activeElement
    dialog.showModal()
    return ()=>{
      dialog.close()
      if(trigger instanceof HTMLElement&&trigger.isConnected)trigger.focus()
    }
  },[])
  return <dialog ref={ref} onCancel={event=>{event.preventDefault();close()}} aria-label={title}><div className="dialog-content stack"><div className="heading"><h2>{title}</h2><button className="icon-button" onClick={close} aria-label="Fermer"><X size={21}/></button></div>{children}</div></dialog>
}
const editable=(role:Role,item:Property,subject:string)=>role==='ADMIN'||String(item.agentId)===subject
export function Properties({role}:{role:Role}){
  const {session}=useAuth(), token=session!.token, id=session!.subject
  const list=useQuery(api.properties,'management-properties')
  const [edit,setEdit]=useState<Property|'new'|null>(null)
  const [remove,setRemove]=useState<Property|null>(null)
  const [busy,setBusy]=useState(false)
  const [message,setMessage]=useState<{error?:string;notice?:string}>({})
  const [deleteError,setDeleteError]=useState('')
  const open=(value:Property|'new')=>{setMessage({});setEdit(value)}
  const deleteListing=async(property:Property)=>{
    setBusy(true)
    setDeleteError('')
    try {
      await api.deleteProperty(property.id,token)
      setRemove(null)
      list.reload()
      try {
        await removeImages(property)
        setMessage({notice:'Annonce supprimée.'})
      }catch(error){
        setMessage({error:'Annonce supprimée sur le serveur. Les photos locales n’ont pas pu être effacées. '+imageError(error)})
      }
    }catch(error){setDeleteError(failure(error))}
    finally{setBusy(false)}
  }
  return <><Heading title="Gestion des annonces" copy={role==='ADMIN'?'Annonces de tous les agents.':'Annonces associées à votre compte.'} action={<button className="button primary" disabled={busy} onClick={()=>open('new')}><Plus size={17} aria-hidden="true"/> Créer une annonce</button>}/>
  <Feedback {...message}/>
  <Query state={list}>{items=>{const own=items.filter(p=>editable(role,p,id));return own.length?<div className="cards">{own.map(p=><Card key={p.id} item={p} action={<div className="inline"><button className="button outline" disabled={busy} onClick={()=>open(p)}><Pencil size={16} aria-hidden="true"/> Modifier</button><button className="button danger" disabled={busy} onClick={()=>{setMessage({});setDeleteError('');setRemove(p)}}><Trash2 size={16} aria-hidden="true"/> Supprimer</button></div>}/>)}</div>:<Empty copy="Aucune annonce à gérer."/>}}</Query>
  {edit&&<Modal title={edit==='new'?'Nouvelle annonce':'Modifier l’annonce'} close={()=>{if(!busy)setEdit(null)}}><ListingEditor item={edit==='new'?undefined:edit} token={token} busy={busy} setBusy={setBusy} reload={list.reload} done={notice=>{setEdit(null);setMessage({notice})}}/></Modal>}
  {remove&&<Modal title="Supprimer cette annonce ?" close={()=>{if(!busy)setRemove(null)}}><p>{remove.title}</p><Feedback error={deleteError}/><button className="button danger" disabled={busy} aria-busy={busy} onClick={()=>void deleteListing(remove)}>Confirmer la suppression</button></Modal>}</>
}
function ListingEditor({item,token,busy,setBusy,reload,done}:{item?:Property;token:string;busy:boolean;setBusy:(busy:boolean)=>void;reload:()=>void;done:(result:string)=>void}){
  const existing=useImages(item??{id:0,agentId:null})
  const [draft,setDraft]=useState<Blob[]|null>(null)
  const [pending,setPending]=useState(false)
  const [issue,setIssue]=useState('')
  const [saved,setSaved]=useState<Property>()
  const photos=draft??(item?existing.photos:[])
  const save=async(input:PropertyInput)=>{
    if(busy||pending)return
    setBusy(true)
    setIssue('')
    try {
      const property=await api.saveProperty(input,token,saved?.id??item?.id)
      setSaved(property)
      reload()
      if(draft!==null){
        try {
          if(!Number.isSafeInteger(property.id)||property.id<1)throw new Error('Identifiant d’annonce absent.')
          await saveImages(property,draft)
        }catch(error){
          setIssue('Annonce enregistrée sur le serveur. Les photos locales n’ont pas été enregistrées. Vous pouvez réessayer avant de fermer. '+imageError(error))
          return
        }
      }
      done(draft===null?'Annonce enregistrée.':'Annonce enregistrée. Galerie locale mise à jour.')
    }catch(error){setIssue(failure(error))}
    finally{setBusy(false)}
  }
  return <PropertyForm item={item} busy={busy||pending} save={save}>
    {item&&existing.loading&&<p role="status">Chargement des photos existantes…</p>}
    {item&&<Feedback error={existing.error}/>}
    <ImageEditor photos={photos} onChange={setDraft} disabled={busy||!!(item&&(existing.loading||existing.error))} pending={pending} onPending={setPending}/>
    <Feedback error={issue}/>
  </PropertyForm>
}

function PropertyForm({item,busy,save,children}:{item?:Property;busy:boolean;save:(data:PropertyInput)=>void;children:ReactNode}){
  return <form className="stack" onSubmit={event=>{const data=formData(event);save({title:value(data,'title'),city:value(data,'city'),description:value(data,'description'),price:Number(value(data,'price')),area:Number(value(data,'area')),propertyType:value(data,'propertyType') as Property['propertyType'],transactionType:value(data,'transactionType') as Property['transactionType']})}}>
  <label>Titre<input name="title" required defaultValue={item?.title}/></label><div className="grid gap-2"><label htmlFor="property-description">Description</label><textarea id="property-description" name="description" required rows={4} defaultValue={item?.description}/></div>
  <div className="form-grid"><label>Ville<input name="city" required defaultValue={item?.city}/></label><label>Prix<input name="price" required type="number" min="0.01" step="0.01" defaultValue={item?.price}/></label><label>Surface en m²<input name="area" required type="number" min="0.01" step="0.01" defaultValue={item?.area}/></label>
  <label>Type de bien<select name="propertyType" defaultValue={item?.propertyType}>{Object.entries(kinds).map(([v,label])=><option key={v} value={v}>{label}</option>)}</select></label>
  <label>Transaction<select name="transactionType" defaultValue={item?.transactionType}>{Object.entries(transactions).map(([v,label])=><option key={v} value={v}>{label}</option>)}</select></label></div>
  {children}<button className="button primary" disabled={busy}>Enregistrer</button></form>
}
function PropertySelect({items,selected}:{items:Property[];selected:string}){
  const id=useId()
  return <div className="grid gap-2"><label htmlFor={id}>Bien</label><select id={id} name="announcementId" defaultValue={items.some(p=>String(p.id)===selected)?selected:''} required><option value="">Choisir une annonce</option>{items.map(p=><option key={p.id} value={p.id}>{p.title} — {p.city}</option>)}</select></div>
}
export function Visits({role}:{role:Role}){
  const {session}=useAuth(), token=session!.token, id=session!.subject
  const [params]=useSearchParams()
  const list=useQuery(()=>api.visits(token,role==='ADMIN'?undefined:role==='BUYER'?'client':'agent',id),'visits:'+id+role)
  const properties=useQuery(()=>role==='BUYER'?api.properties():Promise.resolve([]),'visit-properties:'+role)
  const action=useAction()
  const change=(visit:Visit,status:keyof typeof visitStatus)=>action.run(()=>api.changeVisit(visit.id,status,token),'Statut de visite mis à jour.',list.reload)
  return <><Heading title={role==='BUYER'?'Mes visites':'Gestion des visites'}/>
  <Feedback error={action.error} notice={action.notice}/>
  {role==='BUYER'&&<section className="panel stack"><h2>Réserver une visite</h2><Query state={properties}>{items=>items.length?<form className="form-grid" onSubmit={event=>{
    const form=event.currentTarget,data=formData(event), property=items.find(p=>p.id===Number(value(data,'announcementId')))
    if(!property)return
    void action.run(()=>api.book({announcementId:property.id,clientId:session!.user.id,agentId:property.agentId??undefined,date:value(data,'date'),heure:value(data,'heure')},token),'Visite demandée.',()=>{form.reset();list.reload()})
  }}><PropertySelect items={items} selected={params.get('bien')??''}/><label>Date<input name="date" type="date" required/></label><label>Heure<input name="heure" type="time" required/></label><button className="button primary" disabled={action.busy}>Demander la visite</button></form>:<Empty copy="Une annonce publiée est nécessaire pour réserver."/ >}</Query></section>}
  <Query state={list}>{items=>items.length?table(['Bien','Client','Date et heure','Statut','Actions'],items.map(visit=>[<Link to={'/biens/'+visit.announcementId}>#{visit.announcementId}</Link>,visit.clientId,visit.date+' · '+visit.heure,<StatusBadge status={visit.status}>{visitStatus[visit.status]??visit.status}</StatusBadge>,<div className="inline">{visit.status==='DEMANDEE'&&role!=='BUYER'&&<><button className="button success" disabled={action.busy} onClick={()=>change(visit,'CONFIRMEE')}><Check size={16} aria-hidden="true"/> Confirmer</button><button className="button outline" disabled={action.busy} onClick={()=>change(visit,'REFUSEE')}>Refuser</button></>}{['DEMANDEE','CONFIRMEE'].includes(visit.status)&&<button className="button danger" disabled={action.busy} onClick={()=>change(visit,'ANNULEE')}>Annuler</button>}</div>])):<Empty copy="Aucune visite enregistrée."/>}</Query>
  </>
}
async function agentOffers(token:string,id:string){
  const listings=(await api.properties()).filter(p=>String(p.agentId)===id)
  return (await Promise.all(listings.map(p=>api.offers(token,'announcement',p.id)))).flat()
}
const offerStatuses:Record<Offer['status'],string>={PENDING:'En attente',ACCEPTED:'Acceptée',REJECTED:'Refusée',WITHDRAWN:'Retirée'}
export function Offers({role}:{role:Role}){
  const {session}=useAuth(),token=session!.token,id=session!.subject
  const [params]=useSearchParams()
  const [edit,setEdit]=useState<Offer|null>(null)
  const [remove,setRemove]=useState<Offer|null>(null)
  const [decision,setDecision]=useState<{offer:Offer;type:'accept'|'reject'}|null>(null)
  const canDecide=offerDecisionsEnabled&&(role==='AGENT'||role==='ADMIN')

  const list=useQuery(()=>role==='AGENT'?agentOffers(token,id):api.offers(token,role==='BUYER'?'buyer':undefined,id),'offers:'+id+role)
  const properties=useQuery(()=>role==='BUYER'?api.properties():Promise.resolve([]),'offer-properties:'+role)
  const action=useAction()
  const confirmDecision=()=>{
    if(!decision||action.busy||!canDecide)return
    const {offer,type}=decision
    const accept=type==='accept'
    void action.run(async()=>{
      try {
        const updated=await (accept?api.acceptOffer:api.rejectOffer)(offer.id,token)
        if(updated?.id!==offer.id||updated.status!==(accept?'ACCEPTED':'REJECTED')){
          throw new Error('Le service n’a pas confirmé la décision.')
        }
        list.update(items=>items.map(item=>item.id===updated.id?updated:item))
        setDecision(null)
      }catch(error){
        setDecision(null)
        list.reload()
        throw error
      }
    },accept?'Offre acceptée.':'Offre refusée.')
  }

  const fields=(offer?:Offer)=><><label>Montant proposé<input name="proposedAmount" required type="number" min="0.01" step="0.01" defaultValue={offer?.proposedAmount}/></label><div className="grid gap-2"><label htmlFor={offer?"edited-offer-message":"new-offer-message"}>Message</label><textarea id={offer?"edited-offer-message":"new-offer-message"} name="message" required maxLength={2000} rows={3} defaultValue={offer?.message}/></div></>
  const read=(data:FormData)=>({proposedAmount:Number(value(data,'proposedAmount')),message:value(data,'message')})
  return <><Heading title={role==='BUYER'?'Mes offres':'Offres reçues'} copy={role==='AGENT'?'Offres relatives à vos annonces.':undefined}/>
  <Feedback error={action.error} notice={action.notice}/>
  {role==='BUYER'&&<section className="panel stack"><h2>Soumettre une offre</h2><Query state={properties}>{items=>items.length?<form className="stack" onSubmit={event=>{
    const form=event.currentTarget,data=formData(event)
    void action.run(()=>api.saveOffer(read(data),token,{announcementId:Number(value(data,'announcementId')),buyerId:session!.user.id}),'Offre envoyée.',()=>{form.reset();list.reload()})
  }}><PropertySelect items={items} selected={params.get('bien')??''}/>{fields()}<button className="button primary" disabled={action.busy}>Envoyer l’offre</button></form>:<Empty copy="Une annonce publiée est nécessaire pour soumettre une offre."/ >}</Query></section>}
  <Query state={list}>{items=>items.length?table(['Bien','Acheteur','Montant','Message','Statut',...(role==='BUYER'||canDecide?['Actions']:[])],items.map(offer=>[
    <Link to={'/biens/'+offer.announcementId}>#{offer.announcementId}</Link>,
    offer.buyerId,currency(offer.proposedAmount),offer.message,
    <StatusBadge status={offer.status}>{offerStatuses[offer.status]??offer.status}</StatusBadge>,
    ...(role==='BUYER'?[<div className="inline"><button className="button outline" onClick={()=>setEdit(offer)}>Modifier</button><button className="button danger" onClick={()=>setRemove(offer)}>Supprimer</button></div>]
      :canDecide?[offer.status==='PENDING'?<div className="inline"><button type="button" className="button success" disabled={action.busy} onClick={()=>setDecision({offer,type:'accept'})}><Check size={16} aria-hidden="true"/> Accepter</button><button type="button" className="button danger" disabled={action.busy} onClick={()=>setDecision({offer,type:'reject'})}>Refuser</button></div>:'—']:[])
  ])):<Empty copy="Aucune offre enregistrée."/>}</Query>
  {role!=='BUYER'&&!offerDecisionsEnabled&&<p className="muted">Les décisions sont désactivées pour cet environnement.</p>}
  {decision&&<Modal title={decision.type==='accept'?'Accepter cette offre ?':'Refuser cette offre ?'} close={()=>{if(!action.busy)setDecision(null)}}>
    <p>Offre nº {decision.offer.id} · {currency(decision.offer.proposedAmount)}</p>
    <button type="button" className={'button '+(decision.type==='accept'?'success':'danger')} disabled={action.busy} aria-busy={action.busy} onClick={confirmDecision}>{action.busy?'Envoi…':decision.type==='accept'?'Confirmer l’acceptation':'Confirmer le refus'}</button>
    <button type="button" className="button outline" disabled={action.busy} onClick={()=>setDecision(null)}>Annuler</button>
  </Modal>}
  {edit&&<Modal title="Modifier l’offre" close={()=>setEdit(null)}><Feedback error={action.error}/><form className="stack" onSubmit={event=>{const data=formData(event);void action.run(()=>api.saveOffer(read(data),token,{id:edit.id}),'Offre mise à jour.',()=>{setEdit(null);list.reload()})}}>{fields(edit)}<button className="button primary" disabled={action.busy}>Enregistrer</button></form></Modal>}
  {remove&&<Modal title="Supprimer cette offre ?" close={()=>setRemove(null)}><Feedback error={action.error}/><button className="button danger" disabled={action.busy} onClick={()=>action.run(()=>api.deleteOffer(remove.id,token),'Offre supprimée.',()=>{setRemove(null);list.reload()})}>Confirmer la suppression</button></Modal>}</>
}
