# Frontend

Interfaz de Parking Spot con React 19, TypeScript 5, Vite 7, Tailwind CSS 4 y React Router 7. La ruta `/` muestra la bienvenida y `/vehicles/classify` permite clasificar una placa mediante el backend. La antigua ruta `/classify` redirige a la nueva.

## Requisitos

Node.js 20.19+ o 22.12+ (recomendado: Node.js 22 LTS o superior) y npm 10+.

## Uso

Desde `frontend/`:

```sh
npm install
npm run dev
```

Abrir la URL que indique Vite, normalmente `http://localhost:5173/`. Otros comandos:

```sh
npm run build    # Comprueba TypeScript y genera dist/
npm run preview  # Sirve la compilación localmente
npm run test     # Prueba técnica de renderizado de la ruta /
```

## Estructura

- `src/app/router/`: rutas de la aplicación.
- `src/features/home/`: pantalla inicial.
- `src/features/vehicles/`: formulario y cliente HTTP de clasificación.
- `src/styles/`: estilos globales y entrada de Tailwind CSS.
- `src/tests/`: prueba técnica de renderizado.
- `src/components/`, `hooks/`, `services/`, `types/` y `utils/`: espacios para responsabilidades futuras; aún no contienen código funcional.

`VITE_API_BASE_URL` indica la URL base del backend y vale `http://localhost:8080` por defecto. Puedes copiar `.env.example` a `.env.local` para cambiarla. Arranca también el backend y configura allí `FRONTEND_ORIGIN` con el origen de Vite si difiere de `http://localhost:5173`. La clasificación no guarda datos ni implementa ingreso, salida o autenticación.

## Usar la clasificación con el backend

1. Desde la raíz del repositorio, ejecuta `mvn -f backend/pom.xml spring-boot:run`.
2. En otra terminal, desde `frontend/`, ejecuta `npm run dev`.
3. Abre `http://localhost:5173/vehicles/classify`, escribe una placa y pulsa **Clasificar vehículo**.

El formulario envía `POST /api/v1/vehicles/classify` con JSON `{"plate":"ABC123"}`. Muestra la placa normalizada y el tipo de la respuesta real (`{"plate":"ABC123","type":"CARRO"}`), o el mensaje de error recibido. La validación y clasificación se realizan en el backend. Reinicia Vite si cambias `VITE_API_BASE_URL`.

Si el endpoint responde 404 aunque exista en el código, reinicia el proceso Spring Boot con el código actualizado. Una instancia anterior puede seguir ocupando el puerto 8080; compilar el proyecto no actualiza una JVM que ya está ejecutándose.
