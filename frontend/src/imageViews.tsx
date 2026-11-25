import {useState,type ChangeEvent} from 'react'
import {Building2,ImagePlus,Star,X} from 'lucide-react'
import type {Property} from './api'
import {imageNotice,imageTypes,maxImages,useImages,useImageUrl,verifyImage,type Photo} from './propertyImages'

export function LocalImage({blob,alt,className}:{blob?:Photo;alt:string;className?:string}){
  const url=useImageUrl(blob)
  const [failed,setFailed]=useState<string>()
  return url&&failed!==url
    ?<img src={url} className={className} alt={alt} decoding="async" onError={()=>setFailed(url)}/>
    :<Building2 size={43} aria-hidden="true"/>
}

export function PropertyGallery({property}:{property:Property}){
  const {photos,error}=useImages(property)
  const [selected,select]=useState(0)
  const active=selected<photos.length?selected:0
  return <section className="property-gallery" aria-label="Galerie du bien">
    <div className={'detail-visual'+(photos.length?' has-photo':'')}>
      {photos.length?<LocalImage blob={photos[active]} alt={property.title+' — photo '+(active+1)}/>:<><Building2 size={70} aria-hidden="true"/><span>Photo indisponible</span></>}
    </div>
    {photos.length>1&&<div className="gallery-thumbnails">{photos.map((photo,index)=><button key={index} type="button" aria-label={'Afficher la photo '+(index+1)} aria-pressed={active===index} onClick={()=>select(index)}>
      <LocalImage blob={photo} alt=""/>
    </button>)}</div>}
    {photos.length>0&&<p className="muted local-image-notice">{imageNotice}</p>}
    {error&&<p className="feedback error" role="status">{error}</p>}
  </section>
}

export function ImageEditor({photos,onChange,disabled,pending,onPending}:{photos:Photo[];onChange:(photos:Photo[])=>void;disabled:boolean;pending:boolean;onPending:(pending:boolean)=>void}){
  const [issue,setIssue]=useState('')
  const locked=disabled||pending
  async function choose(event:ChangeEvent<HTMLInputElement>,index?:number){
    const files=Array.from(event.currentTarget.files??[])
    event.currentTarget.value=''
    if(!files.length||locked)return
    setIssue('')
    if(index===undefined&&files.length+photos.length>maxImages){
      setIssue('Vous pouvez enregistrer au maximum cinq images par annonce.')
      return
    }
    onPending(true)
    try {
      const selected=await Promise.all(files.map(verifyImage))
      onChange(index===undefined?[...photos,...selected]:photos.map((photo,position)=>position===index?selected[0]:photo))
    }catch(error){
      setIssue(error instanceof Error?error.message:'Impossible de charger les images.')
    }finally{onPending(false)}
  }
  return <fieldset className="image-editor stack" disabled={locked}>
    <legend>Photos du bien</legend>
    <p className="muted local-image-notice">{imageNotice}</p>
    {photos.length>0&&<div className="image-editor-grid">{photos.map((photo,index)=><div className="image-editor-item" key={photo instanceof Blob?'new-'+index:'id-'+photo.id}>
      <div className="image-editor-preview"><LocalImage blob={photo} alt={'Aperçu de la photo '+(index+1)}/></div>
      <div className="image-editor-actions">
        {index===0?<span className="badge"><Star size={13} aria-hidden="true"/> Couverture</span>:<button className="button outline" type="button" onClick={()=>onChange([photo,...photos.filter((_,pos)=>pos!==index)])}>Définir comme couverture</button>}
        <label className="button outline image-input-label">Remplacer<span className="sr-only"> la photo {index+1}</span><input type="file" accept={imageTypes} onChange={event=>void choose(event,index)}/></label>
        <button type="button" className="icon-button" aria-label={'Retirer la photo '+(index+1)} onClick={()=>onChange(photos.filter((_,pos)=>pos!==index))}><X size={16}/></button>
      </div>
    </div>)}</div>}
    {photos.length<maxImages&&<label className="button outline image-input-label"><ImagePlus size={18}/> Ajouter des photos<input type="file" accept={imageTypes} multiple onChange={event=>void choose(event)}/></label>}
    <p className="muted">{photos.length}/{maxImages} · JPG, PNG ou WebP · 5 Mo maximum par image</p>
    {pending&&<p role="status">Vérification des images…</p>}
    {issue&&<p className="feedback error" role="alert">{issue}</p>}
  </fieldset>
}
