# frontend/Dockerfile
FROM node:20-alpine
WORKDIR /app

COPY package*.json ./
RUN npm install           # lockを使うなら npm ci にして package-lock.json をコミット

COPY . .
RUN npm run build         # dist/ を生成

EXPOSE 5173
CMD ["npm", "run", "preview", "--", "--host", "0.0.0.0", "--port", "5173"]
