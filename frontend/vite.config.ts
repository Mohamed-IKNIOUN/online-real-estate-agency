import {defineConfig,loadEnv,type ProxyOptions} from 'vite'
import react from '@vitejs/plugin-react'
import tailwind from '@tailwindcss/vite'

export default defineConfig(({command,mode}) => {
  const plugins=[react(),tailwind()]
  if(command==='build')return {plugins}

  const env=loadEnv(mode,process.cwd(),'AQAR_PROXY_')
  const proxy:Record<string,ProxyOptions>={}
  const ports={identity:8085,announcements:8081,favourites:8083,visits:8084,offers:8082}
  for(const [service,port] of Object.entries(ports)){
    const key='AQAR_PROXY_'+service.toUpperCase()+'_URL'
    const target=new URL(env[key]||`http://127.0.0.1:${port}`)
    if(!['http:','https:'].includes(target.protocol)||
       !['localhost','127.0.0.1','[::1]'].includes(target.hostname)||
       target.username||target.password||target.pathname!=='/'||target.search||target.hash){
      throw new Error(`${key} doit désigner une origine HTTP(S) locale sans chemin ni identifiants.`)
    }
    const prefix='/backend/'+service
    proxy[prefix]={target:target.origin,changeOrigin:true,rewrite:path=>path.slice(prefix.length)}
  }
  return {plugins,server:{proxy},preview:{proxy}}
})
