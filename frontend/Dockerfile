# Build stage: compile the React/TypeScript app into static files
FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Feature flags are read by Vite at build time (see .env.example)
ARG VITE_FAVOURITES_ENABLED=false
ARG VITE_VISITS_ENABLED=false
ARG VITE_OFFER_DECISIONS_ENABLED=false
ENV VITE_FAVOURITES_ENABLED=$VITE_FAVOURITES_ENABLED \
    VITE_VISITS_ENABLED=$VITE_VISITS_ENABLED \
    VITE_OFFER_DECISIONS_ENABLED=$VITE_OFFER_DECISIONS_ENABLED

RUN npm run build

# Runtime stage: nginx serves the SPA and routes /backend/<service>/ to the APIs
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
