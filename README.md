# TempRoot Android

Aplicación Android de administración y verificación de acceso ROOT existente.

## Compatibilidad
- Android 10 (API 29) o posterior.
- Samsung, Xiaomi/Redmi, Motorola, Realme y otros fabricantes: la app muestra la información del dispositivo y consulta el binario `su` cuando existe.
- La obtención inicial de ROOT no es universal. Depende del modelo exacto, variante, firmware, bootloader y disponibilidad de un método compatible.

## Funciones
- Muestra marca, modelo, versión Android, ABI y compilación.
- Ejecuta `su -c id` para comprobar si un gestor de superusuario concede UID 0.
- Muestra el resultado real de la solicitud, sin fingir que el dispositivo está rooteado.
- Permite abrir Magisk si está instalado.
- No envía información del dispositivo a ningún servidor.

## Importante
Esta aplicación **no** desbloquea bootloaders, no instala exploits, no modifica particiones y no concede ROOT por sí sola. En dispositivos sin ROOT, primero debe realizarse el procedimiento específico y autorizado para ese modelo. Desbloquear el bootloader puede borrar los datos y afectar la garantía o la integridad del dispositivo.

## Compilar
Requisitos: Android Studio con JDK 17 y Android SDK Platform 35. Abrir la carpeta del proyecto y ejecutar `assembleDebug`, o usar el workflow de GitHub Actions incluido.

El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.
