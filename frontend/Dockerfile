# SPA de Angular servida por nginx, que ademas reenvia /api al API Gateway.
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

FROM nginx:1.27-alpine
# La imagen oficial procesa templates/*.template con envsubst al arrancar.
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
ENV GATEWAY_URL=http://gateway:8080
EXPOSE 80
