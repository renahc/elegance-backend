// Local integration fixture. Never enable this issuer in a deployed environment.
import { generateKeyPairSync, createSign } from 'node:crypto';
import http from 'node:http';
const { publicKey, privateKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });
const jwk = { ...publicKey.export({format:'jwk'}), kid:'integration-key', use:'sig', alg:'RS256' };
const base64 = value => Buffer.from(JSON.stringify(value)).toString('base64url');
http.createServer((req,res)=>{
 const url=new URL(req.url,'http://localhost');res.setHeader('Content-Type','application/json');
 if(url.pathname==='/.well-known/jwks.json'){res.end(JSON.stringify({keys:[jwk]}));return;}
 if(url.pathname!=='/token'){res.writeHead(404).end();return;}
 const subject=url.searchParams.get('subject')||'test-client';const role=url.searchParams.get('role');
 const claims={iss:url.searchParams.get('issuer')||'http://elegance-test-issuer',aud:url.searchParams.get('audience')||'elegance-integration-api',sub:subject,oid:subject,name:subject,preferred_username:`${subject}@example.invalid`,iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+(url.searchParams.has("expired")?-3600:3600),...(role?{roles:[role]}:{})};
 const data=`${base64({alg:'RS256',typ:'JWT',kid:jwk.kid})}.${base64(claims)}`;
 const signature=createSign('RSA-SHA256').update(data).sign(privateKey,'base64url');res.end(JSON.stringify({accessToken:`${data}.${signature}`}));
}).listen(19000,'0.0.0.0');
