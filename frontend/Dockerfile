# ==========================================================================
# Frontend EduckTrack - build con Vite y servido por Nginx (RS-10)
# Nginx actua como proxy inverso hacia el backend (RS-01).
# ==========================================================================

# --- Etapa 1: build de la SPA ---
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# --- Etapa 2: servir estaticos con Nginx ---
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY default.conf.template /etc/nginx/templates/default.conf.template
COPY 05-set-resolver.envsh /docker-entrypoint.d/05-set-resolver.envsh
# El entrypoint ignora los scripts sin permiso de ejecucion, y el bit no
# sobrevive a un COPY desde un checkout en Windows.
RUN chmod +x /docker-entrypoint.d/05-set-resolver.envsh

# Valores por defecto para docker-compose; en Railway los sobreescriben las
# variables del servicio (PORT lo inyecta la plataforma y BACKEND_ORIGIN
# apunta al dominio privado del backend).
ENV PORT=80
ENV BACKEND_ORIGIN=http://backend:8080
ENV NGINX_ENVSUBST_FILTER=^(PORT|BACKEND_ORIGIN|NGINX_LOCAL_RESOLVERS)$

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
