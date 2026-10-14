# Frontend React: build con Node y se sirve con nginx sin root. nginx también
# hace de proxy hacia el backend (mismo origen, igual que el proxy de Vite en
# desarrollo), así que no hace falta habilitar CORS.
#
#   docker build -t sneakerfever-frontend frontend

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
