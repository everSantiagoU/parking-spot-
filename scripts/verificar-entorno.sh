#!/usr/bin/env sh
# Verifica que el entorno de docker compose esté arriba y sano.
# Uso (desde la raíz del repositorio): sh scripts/verificar-entorno.sh
set -eu

if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

SERVER_PORT="${SERVER_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-5173}"

echo "1) Base de datos (pg_isready dentro del contenedor)"
docker compose exec -T db pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}"

echo "2) Backend: /actuator/health (debe incluir \"db\" en estado UP)"
curl -fsS "http://localhost:${SERVER_PORT}/actuator/health"
echo

echo "3) Frontend: http://localhost:${FRONTEND_PORT}/"
curl -fsS -o /dev/null -w "HTTP %{http_code}\n" "http://localhost:${FRONTEND_PORT}/"

echo "4) Volumen de datos persistente"
docker volume ls --filter name=parking-spot

echo "Entorno verificado."
