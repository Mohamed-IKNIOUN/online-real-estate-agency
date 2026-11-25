import {useEffect,useState} from 'react'
import {ApiError,request,type Property} from './api'

// Photos are stored by announcements-service and visible to every visitor.
export const imageNotice='Les photos sont enregistrées sur le serveur et visibles par tous les visiteurs.'
export const maxImages=5
export const maxImageBytes=5*1024*1024
export const imageTypes='image/jpeg,image/png,image/webp'
export const imagesConfigured=true

// A photo already on the server, or a new file picked in the editor and not yet uploaded.
export type ServerPhoto={id:number;url:string}
export type Photo=ServerPhoto|Blob
export const isServerPhoto=(photo:Photo):photo is ServerPhoto=>!(photo instanceof Blob)

type PropertyIdentity=Pick<Property,'id'|'agentId'>
type ImageResponse={id:number;url:string;contentType:string;position:number}
const changed='aqar:gallery'
const base='/backend/announcements'
const toPhotos=(items:ImageResponse[]):ServerPhoto[]=>[...items].sort((a,b)=>a.position-b.position).map(item=>({id:item.id,url:base+item.url}))
const path=(id:number)=>'/api/announcements/'+id+'/images'

export async function loadImages(property:PropertyIdentity):Promise<ServerPhoto[]>{
  if(!Number.isSafeInteger(property.id)||property.id<1)return []
  return toPhotos(await request<ImageResponse[]>('announcements',path(property.id)))
}

// Replaces the whole gallery in display order (first photo = cover).
export async function saveImages(property:PropertyIdentity,photos:Photo[],token:string):Promise<ServerPhoto[]>{
  if(photos.length>maxImages)throw new Error('Vous pouvez enregistrer au maximum cinq images par annonce.')
  const form=new FormData()
  const layout:string[]=[]
  let index=0
  for(const photo of photos){
    if(isServerPhoto(photo)){layout.push('e:'+photo.id);continue}
    if(photo.size>maxImageBytes||!imageTypes.split(',').includes(photo.type))throw new Error('Sélection d’images invalide.')
    form.append('files',photo,'photo-'+index+'.'+photo.type.split('/')[1])
    layout.push('f:'+index++)
  }
  form.append('layout',layout.join(','))
  const saved=toPhotos(await request<ImageResponse[]>('announcements',path(property.id),'PUT',form,token,60000))
  window.dispatchEvent(new CustomEvent(changed,{detail:property.id}))
  return saved
}

// The server deletes the photos together with the listing.
export const removeImages=(_property:PropertyIdentity)=>Promise.resolve()

export function imageError(error:unknown){
  if(error instanceof ApiError||error instanceof Error)return error.message
  return 'Les photos n’ont pas pu être enregistrées. Réessayez.'
}

export async function verifyImage(file:File){
  if(!imageTypes.split(',').includes(file.type))throw new Error('Formats acceptés : JPG, PNG et WebP.')
  if(file.size===0||file.size>maxImageBytes)throw new Error('Chaque image doit être non vide et ne pas dépasser 5 Mo.')
  const blob=file.slice(0,file.size,file.type)
  try {
    const signature=new Uint8Array(await blob.slice(0,12).arrayBuffer())
    const text=String.fromCharCode(...signature)
    const recognised=file.type==='image/jpeg'
      ?signature[0]===0xff&&signature[1]===0xd8&&signature[2]===0xff
      :file.type==='image/png'
        ?[137,80,78,71,13,10,26,10].every((byte,index)=>signature[index]===byte)
        :text.startsWith('RIFF')&&text.slice(8,12)==='WEBP'
    if(!recognised)throw new Error()
    const preview=await createImageBitmap(blob)
    preview.close()
    return blob
  } catch {
    throw new Error('Impossible de décoder cette image. Sélectionnez un fichier JPG, PNG ou WebP valide.')
  }
}

export function useImages(property:PropertyIdentity){
  const {id,agentId}=property
  const key=String(id)
  const [revision,reload]=useState(0)
  const [state,setState]=useState<{key:string;photos:ServerPhoto[];loading:boolean;error?:string}>({key,photos:[],loading:true})
  useEffect(()=>{
    const update=(event:Event)=>{if((event as CustomEvent<number>).detail===id)reload(value=>value+1)}
    window.addEventListener(changed,update)
    return()=>window.removeEventListener(changed,update)
  },[id])
  useEffect(()=>{
    let current=true
    setState(previous=>({key,photos:previous.key===key?previous.photos:[],loading:true}))
    loadImages({id,agentId}).then(photos=>{
      if(current)setState({key,photos,loading:false})
    }).catch(error=>{
      if(current)setState({key,photos:[],loading:false,error:'Photos indisponibles. '+imageError(error)})
    })
    return()=>{current=false}
  },[id,agentId,key,revision])
  return state.key===key?state:{photos:[] as ServerPhoto[],loading:true,error:undefined}
}

// Server photos already have a URL; new files get a temporary object URL for the preview.
export function useImageUrl(photo?:Photo){
  const blob=photo&&!isServerPhoto(photo)?photo:undefined
  const [state,setState]=useState<{blob:Blob;url:string}|null>(null)
  useEffect(()=>{
    if(!blob){setState(null);return}
    const url=URL.createObjectURL(blob)
    setState({blob,url})
    return()=>URL.revokeObjectURL(url)
  },[blob])
  if(photo&&isServerPhoto(photo))return photo.url
  return state?.blob===blob?state?.url:undefined
}
