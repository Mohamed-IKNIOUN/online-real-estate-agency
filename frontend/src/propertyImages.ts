import {useEffect,useState} from 'react'
import type {Property} from './api'

export const imageNotice='Images enregistrées localement dans ce navigateur uniquement. Elles ne sont pas stockées sur le serveur.'
export const maxImages=5
export const maxImageBytes=5*1024*1024
export const imageTypes='image/jpeg,image/png,image/webp'
const testEnvironment=String(import.meta.env.VITE_LOCAL_IMAGE_ENV??'').trim()
export const imagesConfigured=import.meta.env.DEV&&/^[A-Za-z0-9_-]{8,128}$/.test(testEnvironment)
const storeName='galleries'
const changed='maison-adresse:gallery'
let connection:Promise<IDBDatabase>|undefined

type PropertyIdentity=Pick<Property,'id'|'agentId'>
const keyFor=({id,agentId}:PropertyIdentity)=>JSON.stringify([testEnvironment,id,agentId])

function database(){
  if(!connection){
    connection=new Promise<IDBDatabase>((resolve,reject)=>{
      const request=indexedDB.open('maison-adresse-images',1)
      request.onupgradeneeded=()=>request.result.createObjectStore(storeName)
      request.onerror=()=>reject(request.error)
      request.onblocked=()=>reject(new Error('Base d’images bloquée par un autre onglet.'))
      request.onsuccess=()=>{
        const db=request.result
        db.onversionchange=()=>{db.close();connection=undefined}
        resolve(db)
      }
    }).catch(error=>{connection=undefined;throw error})
  }
  return connection
}
async function transact<T>(mode:IDBTransactionMode,operation:(store:IDBObjectStore)=>IDBRequest<T>):Promise<T>{
  const db=await database()
  return new Promise((resolve,reject)=>{
    const tx=db.transaction(storeName,mode)
    const request=operation(tx.objectStore(storeName))
    tx.oncomplete=()=>resolve(request.result)
    tx.onabort=()=>reject(request.error??tx.error??new Error('Transaction interrompue.'))
  })
}

export async function loadImages(property:PropertyIdentity):Promise<Blob[]>{
  if(!imagesConfigured)return []
  return (await transact('readonly',store=>store.get(keyFor(property))))??[]
}
export async function saveImages(property:PropertyIdentity,photos:Blob[]){
  if(!imagesConfigured)throw new Error('Identifiant d’environnement d’images manquant.')
  if(photos.length>maxImages||photos.some(blob=>blob.size>maxImageBytes||!imageTypes.split(',').includes(blob.type))){
    throw new Error('Sélection d’images invalide.')
  }
  const key=keyFor(property)
  if(photos.length)await transact('readwrite',store=>store.put(photos,key))
  else await transact('readwrite',store=>store.delete(key))
  window.dispatchEvent(new CustomEvent(changed,{detail:key}))
}
export const removeImages=(property:PropertyIdentity)=>imagesConfigured?saveImages(property,[]):Promise.resolve()

export function imageError(error:unknown){
  return error instanceof DOMException&&error.name==='QuotaExceededError'
    ?'Espace de stockage insuffisant. Libérez de la place ou retirez des photos, puis réessayez.'
    :'Le stockage local des images est indisponible. Vérifiez les autorisations du navigateur ou fermez les autres onglets, puis réessayez.'
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
  const key=keyFor(property)
  const [revision,reload]=useState(0)
  const [state,setState]=useState<{key:string;photos:Blob[];loading:boolean;error?:string}>({key,photos:[],loading:imagesConfigured})
  useEffect(()=>{
    const update=(event:Event)=>{if((event as CustomEvent<string>).detail===key)reload(value=>value+1)}
    window.addEventListener(changed,update)
    return()=>window.removeEventListener(changed,update)
  },[key])
  useEffect(()=>{
    let current=true
    if(!imagesConfigured)return
    setState(previous=>({key,photos:previous.key===key?previous.photos:[],loading:true}))
    void loadImages({id,agentId}).then(photos=>{
      if(current)setState({key,photos,loading:false})
    }).catch(error=>{
      if(current)setState({key,photos:[],loading:false,error:imageError(error)})
    })
    return()=>{current=false}
  },[id,agentId,key,revision])
  return state.key===key?state:{photos:[],loading:imagesConfigured,error:undefined}
}

export function useImageUrl(blob?:Blob){
  const [state,setState]=useState<{blob:Blob;url:string}|null>(null)
  useEffect(()=>{
    if(!blob){setState(null);return}
    const url=URL.createObjectURL(blob)
    setState({blob,url})
    return()=>URL.revokeObjectURL(url)
  },[blob])
  return state?.blob===blob?state?.url:undefined
}
