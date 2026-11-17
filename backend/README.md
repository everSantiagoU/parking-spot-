# Backend

Backend Java 21 y Spring Boot, preparado como monolito modular con separación hexagonal. Los puertos se ubican dentro del dominio (`domain/port`) porque representan contratos del núcleo; los adaptadores concretos residirán en infraestructura. No se incluyen contratos vacíos ni lógica de negocio.

Para compilar cuando Maven y Java 21 estén disponibles: `mvn -f backend/pom.xml package`.
