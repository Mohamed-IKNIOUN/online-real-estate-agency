import axios from 'axios'

export type Role='BUYER'|'AGENT'|'ADMIN'
export type User={id:number; fullName:string; email:string; role:Role}
export type Login=User & {accessToken:string; refreshToken:string}
export const kinds={APARTMENT:'Appartement',HOUSE:'Maison',VILLA:'Villa',LAND:'Terrain',COMMERCIAL:'Local commercial'}
export const transactions={SALE:'Vente',RENT:'Location'}
export type PropertyInput={title:string; description:string; city:string; price:number; area:number; propertyType:keyof typeof kinds; transactionType:keyof typeof transactions}
export type Property=PropertyInput & {id:number; agentId:number|null}
export type Favourite={id:number; annonceId:number; clientId:number}
export const visitStatus={DEMANDEE:'Demandée',CONFIRMEE:'Confirmée',REFUSEE:'Refusée',ANNULEE:'Annulée'}
export type VisitInput={announcementId:number; clientId:number; agentId?:number; date:string; heure:string}
export type Visit=VisitInput & {id:number; status:keyof typeof visitStatus}
export type OfferStatus='PENDING'|'ACCEPTED'|'REJECTED'|'WITHDRAWN'
export type OfferInput={message:string; proposedAmount:number}
export type Offer=OfferInput & {id:number; announcementId:number; buyerId:number; status:OfferStatus; submittedAt:string}
export type Service='identity'|'announcements'|'favourites'|'visits'|'offers'
const http=axios.create({timeout:9000})
export class ApiError extends Error {
  constructor(public status:number, public service:Service, message:string) {super(message)}
}
const messages:Record<number,string>={413:'Fichier trop volumineux.',400:'Vérifiez les champs saisis.',401:'Connexion requise ou identifiants invalides.',403:'Votre compte ne dispose pas de cet accès.',404:'La ressource demandée est introuvable.',409:'Cette opération entre en conflit avec les données existantes.',422:'Les données envoyées ne peuvent pas être traitées.',500:'Le service a rencontré une erreur interne.',503:'Le service est temporairement indisponible. Réessayez plus tard.'}
export async function request<T>(service:Service,path:string,method='GET',data?:unknown,token?:string,timeout?:number):Promise<T>{
  try {
    const response=await http.request<T>({url:'/backend/'+service+path,method,data,timeout,headers:token?{Authorization:'Bearer '+token}:undefined})
    return response.data
  } catch (err) {
    if (!axios.isAxiosError(err)) throw err
    const status=err.response?.status ?? 0
    const payload:unknown=err.response?.data
    const detail=typeof payload==='object' && payload!==null && 'message' in payload && typeof payload.message==='string' && payload.message.length<240 && [400,409,413,422].includes(status) ? ' '+payload.message : ''
    throw new ApiError(status,service,!status || [502,504].includes(status) || (status===500 && !payload) ? 'Service '+service+' indisponible. Vérifiez son URL et son démarrage.' : (messages[status] ?? 'Requête refusée ('+status+').')+detail)
  }
}
export const api={
  login:(email:string,password:string)=>request<Login>('identity','/api/auth/login','POST',{email,password}),
  register:(data:{email:string;password:string;fullName:string;role:'BUYER'|'AGENT'})=>request<User>('identity','/api/auth/register','POST',data),
  me:(token:string)=>request<User>('identity','/api/users/me','GET',undefined,token),
  logout:(refreshToken:string)=>request<void>('identity','/api/auth/logout','POST',{refreshToken}),
  properties:()=>request<Property[]>('announcements','/api/announcements'),
  property:(id:number)=>request<Property>('announcements','/api/announcements/'+id),
  saveProperty:(data:PropertyInput,token:string,id?:number)=>request<Property>('announcements','/api/announcements'+(id?'/'+id:''),id?'PUT':'POST',data,token),
  deleteProperty:(id:number,token:string)=>request<void>('announcements','/api/announcements/'+id,'DELETE',undefined,token),
  favourites:(clientId:string,token:string)=>request<Favourite[]>('favourites','/api/favoris/client/'+clientId,'GET',undefined,token),
  setFavourite:(clientId:string,id:number,token:string,remove=false)=>request<void>('favourites','/api/favoris/'+clientId+'/'+id,remove?'DELETE':'POST',undefined,token),
  visits:(token:string,scope?:'client'|'agent',id?:string)=>request<Visit[]>('visits','/api/visits'+(scope?'/'+scope+'/'+id:''),'GET',undefined,token),
  book:(input:VisitInput,token:string)=>request<Visit>('visits','/api/visits','POST',input,token),
  changeVisit:(id:number,status:keyof typeof visitStatus,token:string)=>request<Visit>('visits','/api/visits/'+id+'/status?status='+status,'PATCH',undefined,token),
  offers:(token:string,scope?:'buyer'|'announcement',id?:string|number)=>request<Offer[]>('offers','/api/offers'+(scope?'/'+scope+'/'+id:''),'GET',undefined,token),
  offer:(id:number,token:string)=>request<Offer>('offers','/api/offers/'+id,'GET',undefined,token),
  saveOffer:(input:OfferInput,token:string,identity:{announcementId:number;buyerId:number}|{id:number})=>'id' in identity
    ? request<Offer>('offers','/api/offers/'+identity.id,'PUT',input,token)
    : request<Offer>('offers','/api/offers','POST',{...input,...identity},token),
  acceptOffer:(id:number,token:string)=>request<Offer>('offers','/api/offers/'+id+'/accept','PUT',undefined,token),
  rejectOffer:(id:number,token:string)=>request<Offer>('offers','/api/offers/'+id+'/reject','PUT',undefined,token),

  deleteOffer:(id:number,token:string)=>request<void>('offers','/api/offers/'+id,'DELETE',undefined,token)
}
export const failure=(error:unknown)=>error instanceof Error?error.message:'Une erreur est survenue.'
export const currency=(amount:number)=>new Intl.NumberFormat('fr-FR',{style:'currency',currency:'MAD',currencyDisplay:'code',minimumFractionDigits:Number.isInteger(amount)?0:2,maximumFractionDigits:2}).format(amount)
