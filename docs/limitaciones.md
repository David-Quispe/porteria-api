# Limitaciones y condiciones para una instalación real

Este texto complementa los [diagramas](diagramas.md) del informe. Describe riesgos del prototipo; no acredita por sí solo que una instalación física o el tratamiento de datos personales estén autorizados.

## Alcance del lector NFC en vehículos

El PN532 es un lector de proximidad. NXP indica para el chip una distancia típica de hasta 70 mm, que depende de antena, etiqueta y entorno. Por tanto, un sticker dentro de un vehículo puede quedar fuera de alcance por la distancia al lector, metal o parabrisas. Antes de usarlo en una garita vehicular hay que medir la distancia real con la antena y posición definitivas. Si el conductor debe acercar el sticker manualmente, el sistema no es una lectura automática a distancia.

## Identificación por UID

La versión actual identifica stickers por su UID, sin un desafío criptográfico. Un UID observado o reproducido no demuestra por sí mismo que la etiqueta física sea original. El token del ESP32 autentica al dispositivo frente a la API, pero no autentica la etiqueta. Para un acceso de mayor riesgo habría que elegir etiquetas y lectores con autenticación criptográfica, verificarla en firmware y conservar la supervisión humana de la barrera. El DNI impreso en Code39 y los QR estáticos también pueden copiarse; no deben tratarse como secretos.

## Datos personales

La API almacena nombres, DNI, fotos y registros de entradas/salidas. Antes de utilizar datos reales, el responsable del sistema debe definir finalidad, base para el tratamiento, información a titulares, plazos de conservación, acceso por roles, atención de derechos y gestión de incidentes conforme a la Ley peruana N.º 29733 y su reglamento vigente (D. S. N.º 016-2024-JUS). Los datos de demostración son ficticios y solo se cargan en el perfil `dev`; el perfil `prod` exige secretos propios. La aplicación ya restringe fotos y endpoints por rol, pero no implementa por sí sola todas las obligaciones organizativas o legales.

## Validación pendiente fuera del código

- Probar el ESP32 con PN532, GM65, botón, servo, tres credenciales reales y fallas de WiFi/API; medir latencia y alimentación.
- Ejecutar `docker compose` y la demostración completa en una segunda máquina y red, con un origen HTTPS para el frontend.
- Revisar el montaje de la barrera y un procedimiento seguro ante cortes de energía; el servo de demostración no sustituye un controlador de barrera certificado.

## Fuentes

- [NXP PN532: distancia típica de lectura](https://www.nxp.com/products/rfid-nfc/nfc-hf/nfc-integrated-solution%3APN5321A3HN).
- [NXP NTAG213/215/216: mecanismos de firma y seguridad disponibles en el chip](https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf).
- [Ley N.º 29733 en la plataforma del Estado peruano](https://www.gob.pe/institucion/congreso-de-la-republica/normas-legales/243470).
- [Reglamento D. S. N.º 016-2024-JUS](https://www.gob.pe/institucion/anpd/normas-legales/6554453-16-2024-jus).
