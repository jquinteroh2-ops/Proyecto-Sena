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
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
