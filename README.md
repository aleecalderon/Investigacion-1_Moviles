# 🍅 Aplicación Pomodoro - Gestión de Tareas

## 📖 Descripción

Aplicación móvil desarrollada en **Android Studio** utilizando **Kotlin** y **XML**, basada en la técnica de productividad **Pomodoro**.

La aplicación permite administrar tareas diarias mediante un temporizador de 25 minutos, facilitando la organización del trabajo y el seguimiento de las sesiones completadas.

---

## 🎯 Objetivos

- Administrar tareas de forma dinámica.
- Aplicar la técnica Pomodoro.
- Registrar sesiones completadas.
- Mostrar el progreso del usuario.
- Mantener las tareas guardadas mediante persistencia de datos.

---

## ✨ Funcionalidades

- ✅ Agregar tareas.
- ✅ Validación de tareas vacías.
- ✅ Validación de tareas duplicadas.
- ✅ Seleccionar una tarea activa.
- ✅ Marcar tareas como completadas.
- ✅ Eliminar tareas.
- ✅ Temporizador Pomodoro (25 minutos).
- ✅ Iniciar, pausar, reanudar y reiniciar temporizador.
- ✅ Barra de progreso.
- ✅ Historial de sesiones Pomodoro.
- ✅ Resumen de tareas pendientes y sesiones completadas.
- ✅ Persistencia de tareas mediante SharedPreferences.
- ✅ Mensajes cuando no existen tareas o historial.

---

## 🛠 Tecnologías utilizadas

- Android Studio
- Kotlin
- XML
- View Binding
- SharedPreferences
- CountDownTimer

---

## 📂 Estructura del proyecto

```
app/
│
├── java/
│   ├── MainActivity.kt
│   └── Task.kt
│
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── item_task.xml
│   │   └── item_history.xml
│   │
│   ├── values/
│   └── drawable/
│
└── AndroidManifest.xml
```

---

## 📱 Características principales

### Gestión de tareas

- Agregar nuevas tareas.
- Seleccionar tarea activa.
- Marcar tareas completadas.
- Eliminar tareas.
- Actualización automática de la interfaz.

### Temporizador Pomodoro

- Temporizador de 25 minutos.
- Barra de progreso.
- Controles de:
  - Iniciar
  - Pausar
  - Reanudar
  - Reiniciar

### Historial

- Registro automático de sesiones completadas.
- Asociación de cada sesión con la tarea activa.

### Persistencia

Las tareas permanecen almacenadas utilizando **SharedPreferences**, permitiendo conservar la información al cerrar y volver a abrir la aplicación.

---

## 👥 Integrantes del equipo

- **Enrique Alexander Solano López** — **SL223188**
- **Gladis del Carmen Rivas Miranda** — **RM191684**
- **Geisel Gabriela Castellanos Flores** — **CF241034**
- **Alejandra Cristal Calderón Escobar** — **CE231635**
- **Francisco Armando Morales Flores** — **MF230357**

---

## 👨‍💻 Asignatura

**Desarrollo de Aplicaciones Móviles**

Universidad de El Salvador

2026
