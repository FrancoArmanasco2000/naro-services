# Workflows de publicación de imágenes

## Estado actual

Un archivo por servicio (`publish-auth-service.yml`, `publish-user-service.yml`),
mismo patrón en los dos: al mergear a `main` tocando la carpeta del servicio,
compila el jar, buildea la imagen y la publica en Docker Hub
(`armanasco2000/naro-<servicio>`) con tag `latest` y con el hash del commit.

Con dos servicios así, cada archivo se lee de un vistazo y no vale la pena
la abstracción de una matriz.

## Cuándo migrar a una matriz

Al agregar el **próximo** servicio nuevo (el tercero con imagen propia en
Docker Hub — hoy `eureka-server`, `config-server` y `api-gateway` ya se
publican pero no tienen workflow propio, así que en la práctica esto aplica
apenas se sume otro servicio que, como `auth-service`/`user-service`, se
espera que cambie seguido), reemplazar los archivos individuales por uno
solo con `strategy.matrix`, algo así:

```yaml
on:
  push:
    branches: [main]
    paths:
      - 'auth-service/**'
      - 'user-service/**'
      - 'nuevo-servicio/**'

jobs:
  build-and-push:
    strategy:
      matrix:
        service: [auth-service, user-service, nuevo-servicio]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
          cache-dependency-path: ${{ matrix.service }}/pom.xml
      - run: |
          cd ${{ matrix.service }}
          chmod +x mvnw
          ./mvnw clean package -DskipTests
      - uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: ./${{ matrix.service }}
          push: true
          tags: |
            armanasco2000/naro-${{ matrix.service }}:latest
            armanasco2000/naro-${{ matrix.service }}:${{ github.sha }}
```

Para no reconstruir los 5 servicios en cada push, sumar un filtro de paths
por servicio (por ejemplo con `dorny/paths-filter`) que arme la lista de
`matrix.service` solo con lo que efectivamente cambió.

Agregar un servicio nuevo, en ese esquema, pasa a ser una línea más en la
lista `service:` y una línea más en `paths:` — no un archivo nuevo.
