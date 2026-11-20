# Estudihambre Life — Plan de Arquitectura, Diseño y Añadidos

**Estudihambre Life** es una aplicación Android offline-first de seguimiento calórico, proteico y de composición corporal impulsada por Gemini multimodal, que combina cálculo geométrico de recipientes, termodinámica aplicada y una identidad visual inspirada en el diario culinario de *Dungeon Meshi*, la cocina de aventura de *Zelda: Breath of the Wild* y la calidez artesanal de *Stardew Valley* unida a las matemáticas exactas.

---

## Revisión del Usuario y Decisiones Críticas

> [!IMPORTANT]
> Revisa las decisiones confirmadas y elige qué añadidos recomendados quieres incluir antes de iniciar la construcción. Si deseas ajustar algún punto, indícalo en los comentarios de revisión.

- **Nombre e Identidad Confirmados**: **Estudihambre Life** (`17` caracteres, compatible con políticas de Play Store). Ícono adaptativo personalizado que fusiona un caldero/plato estilo *Dungeon Meshi / Zelda BotW* con trazos geométricos, símbolos matemáticos ($\pi, \Sigma, \Delta$) y píxeles cálidos estilo *Stardew Valley*.
- **Mejoras Técnicas Confirmadas**: Motor `.xlsx` OOXML nativo en Kotlin puro, snapshot diario de TDEE y pisos en el historial, almacenamiento privado en `filesDir` con limpieza automática tras sincronizar, y comidas favoritas + recipientes de llenado rápido.
- **Motor de IA por Defecto (Firebase AI Logic)**: Se usará **Firebase AI Logic (`Firebase.ai`) con `gemini-3.8-flash`** (sin API key en el APK ni prompts inseguros; `gemini-2.5-flash` está deprecado en este entorno) con salida JSON estructurada (`Schema.obj` + `responseMimeType = "application/json"`). *Nota: Si insistes en que cada usuario pegue su propia API key manual vía Retrofit en Ajustes en lugar de Firebase AI Logic, indícalo antes de aprobar.*
- **Añadidos Recomendados (Propuestos para Estudihambre Life)**:
  1. **Modo "Olla de Aventurero" (Batch Cooking / Dividir en $N$ porciones)**: Al cocinar una olla grande para toda la semana (típico de vida *estudihambre*), tomas una sola foto/descripción del guiso completo o usas un recipiente grande, defines en cuántas porciones/tuppers lo divides ($1/N$) y la app calcula calorías, proteína y costo calórico exacto por porción.
  2. **Índice "Proteína por Peso" y Etiqueta de Supervivencia Estudihambre**: Campo opcional de costo aproximado por comida (en moneda local) para que la app muestre qué comidas te dan mayor **gramos de proteína por moneda** y un resumen semanal de eficiencia nutricional vs. presupuesto.
  3. **Sistema de "Buffs y Estados Metabólicos" (Estilo Zelda / Dungeon Meshi)**: Traducir los diagnósticos fríos en medallas/estados visuales legibles junto al dato clínico — por ejemplo: *Elixir Anabólico* (piso proteico cumplido + rango óptimo), *Escudo de Glucógeno* (retención hídrica detectada sin grasa real), o *Maleficio Catabólico* (deuda acumulada que exige recarga).
  4. **Escáner / Comparador "Antojo vs. Presupuesto del Día"**: En el modo de Simulación Previa, mostrar un deslizador rápido de fracción ($25\%, 50\%, 75\%, 100\%$) que recalcula en tiempo real sin volver a llamar a Gemini si el ajuste es proporcional, ahorrando cuota y tiempo.

---

## 1. Visión General y Concepto Central

- **Qué hace**: Permite registrar comidas mediante foto (CameraX o Photo Picker), texto o volumen geométrico de recipientes propios; simular el impacto antes de comer; gestionar días con margen de error base, piso anti-catabólico y deuda catabólica entre jornadas; filtrar fluctuaciones hídricas comparando peso real vs. peso teórico seco; auto-calibrar el gasto energético real tras 14–21 días; consultar y filtrar la Tabla Maestra; simular proyecciones de déficit; trabajar 100% sin conexión con cola diferida (`WorkManager`); y exportar/importar libros `.xlsx` compatibles.
- **Público objetivo**: Estudiantes, deportistas y personas con presupuesto ajustado o seguimiento riguroso que buscan precisión matemática en su déficit calórico sin sacrificar masa muscular ni depender de internet constante.
- **Valor diferencial**: Une rigor termodinámico (geometría de recipientes, peso teórico seco, memoria de deuda catabólica y calibración de gasto real) con fricción mínima de registro y una estética lúdica tipo manual de campo gastronómico-matemático.

---

## 2. Experiencia de Usuario y Diseño Visual

### Flujos Clave de Usuario
1. **Primera Apertura y Perfil Biométrico**: Onboarding breve hacia la configuración de perfil (peso actual, estatura, edad, sexo, peso objetivo manual, rutina cotidiana para TDEE, rutina de fuerza, presupuesto de error inicial por defecto ej. `+136 kcal` y proteína mínima en `g/kg`) con cálculo en vivo del Rango Óptimo (piso y techo).
2. **Tablero Diario (*El Caldero de Hoy*)**: Muestra los medidores de rango óptimo (acumulado, faltante para piso seguro, restante antes del techo), barra de proteína en `g` y `g/kg`, banner prioritario si existe **Deuda Catabólica** heredada de días previos, estado de cola offline (`PENDING_SYNC`), lista de comidas del día (con acciones rápidas para editar, eliminar o mover al día anterior por madrugadas) y botón explícito **"Cerrar Día"** para consolidar la `Nota Biológica`.
3. **Registro Multimodal y Simulador Previo (*Mesa de Alquimia*)**: Captura rápida con **CameraX** (compresión eficiente JPEG/WebP a máx. `1024px`) o selección sin permisos invasivos vía **Photo Picker**, descripción de texto, selector de **Recipiente + nivel de llenado ($25\%, 50\%, 75\%, 100\%$)** o comidas favoritas. El botón **"Simular / Evaluar impacto"** muestra el JSON de Gemini + vista previa del balance resultante del día + alertas (sodio, azúcar, fritura, densidad proteica) + ajuste de porción antes de confirmar al día o guardar sin internet en la cola offline.
4. **Gestor de Recipientes (*Arsenal Geométrico*)**: CRUD de tuppers, bowls y vasos con cálculo instantáneo de mililitros en vivo para formas **Cilíndrica** ($V = \pi r^2 h$), **Rectangular** ($V = l \times a \times h$) y **Semiesférica** ($\frac{2}{3}\pi r^3$), marcando favoritos para llenado en un toque.
5. **Pesajes, Agua y Simulador de Déficit (*Báscula & Oráculo*)**: Registro histórico con filtro de seguridad ante saltos abruptos ($\pm 3.0\,\text{kg}$ en 24h), comparativa **Peso Real vs. Media Móvil 7 días vs. Peso Teórico Seco**, lectura de retención hídrica/glucógeno en gramos, alertas de efecto *whoosh*, panel de **Auto-calibración termodinámica** (14–21 días), y **Simulador interactivo** con slider de déficit + gráfica nativa Canvas de proyección a $X$ semanas y alertas de riesgo catabólico.
6. **Tabla Maestra y Excel I/O (*Libro de Crónicas*)**: Tabla desplazable bidireccional con las 8 columnas exactas (`Día | Fecha | Ingesta (kcal) | Gasto (kcal) | Déficit (kcal) | Peso (kg) | Proteína (g) | Nota Biológica`), filtros rápidos (*Desde último pesaje*, *7 / 14 / 30 días*, *Rango personalizado*), tarjeta de estadísticas acumuladas del intervalo (grasa teórica oxidada vs. variación real en báscula) y exportación/importación `.xlsx` vía SAF.

### Identidad Visual y Tema
- **Dirección Estética**: *"Manual de Alquimia Culinaria y Cálculo"* — fusión de pizarra oscura de hierro fundido y pergamino cálido (*Dungeon Meshi*), indicadores vitales luminosos (*Zelda BotW*) y calidez artesanal de granja (*Stardew Valley*), con diagramación matemática nítida.
- **Paleta de Colores (Material 3 Tokens)**:
  - Fondo principal (`background`): Pizarra cálida nocturna `#141210` (Hierro fundido) / Superficies (`surface`, `surfaceContainer`): `#1E1A16` y `#29231D` (Madera ahumada).
  - Primario (`primary`): Dorado Miel / Caldo Épico `#E5A93C` (acciones principales, acento de caldero).
  - Secundario (`secondary`): Verde Hierba Hyrule / Esmeralda Botánica `#52B788` (rango óptimo, proteína cumplida, estado saludable).
  - Terciario (`tertiary`): Azul Agua Pura / Cristal `#4EA8DE` (peso teórico seco, métricas hídricas y geometría).
  - Error / Alerta Catabólica (`error`): Carmesí Brasa `#E05A47` (deuda catabólica, exceso de déficit o alertas de sodio/azúcar).
  - Texto sobre superficie (`onSurface`): Pergamino Crema `#F4ECE1` y `#B8ACA0` para metadatos.
- **Tipografía y Jerarquía**: Fuentes locales instaladas vía `font-util` en `res/font/`: **`Fraunces`** (display con carácter editorial de libro de recetas medieval/aventurero para títulos y nombres de platos), **`Plus Jakarta Sans`** (cuerpo claro y legible en español) y **`JetBrains Mono`** (tabular numbers para kcal, gramos, mililitros, fórmulas geométricas y columnas de la Tabla Maestra).
- **Arte e Iconografía**: Ícono adaptativo generado con estética ilustrada cálida (olla/caldero con ingredientes pixel-art/pintados estilo *Stardew / Dungeon Meshi* y compás/fórmulas matemáticas sutiles sobre fondo sólido `#1E1A16`) más 2–3 ilustraciones estáticas para estados vacíos (caldero vacío y pergamino de tabla vacía).

---

## 3. Decisiones Clave de Producto y Trade-Offs

- **Integración con Gemini (Keyless Firebase AI Logic vs. API Key manual)**:
  - *Enfoque elegido*: **Firebase AI Logic (`Firebase.ai`) con App Check y `gemini-3.8-flash`** como motor principal, garantizando respuesta JSON determinista con `Schema.obj(...)` y `responseMimeType = "application/json"`.
  - *Por qué*: Evita exponer API keys en APKs decompilables, elimina la fricción de configurar claves manuales para usar la app y usa un modelo vigente soportado (`gemini-2.5-flash` ya está deprecado).
  - *Alternativa considerada*: Cliente Retrofit + `EncryptedSharedPreferences` para clave del usuario; se descarta por defecto para priorizar seguridad Zero-Trust, salvo que el usuario lo solicite explícitamente en revisión.

- **Motor Excel `.xlsx` (OOXML en Kotlin Puro vs. Apache POI)**:
  - *Enfoque elegido*: **Escritor/lector OOXML nativo en Kotlin** usando `ZipOutputStream` / `ZipInputStream` + `XmlPullParser` integrado con Storage Access Framework (`CreateDocument` / `OpenDocument`).
  - *Por qué*: Apache POI añade >15 MB al APK, exige dependencias XML pesadas incompatibles con `minSdk 24` sin desugaring complejo y consume mucha memoria. Un motor OOXML minimalista lee y escribe libros `.xlsx` estándar de 8 columnas en milisegundos con cero dependencias externas.
  - *Alternativa considerada*: Apache POI o CSV simple; CSV rompe la compatibilidad nativa solicitada con libros `.xlsx`, mientras que OOXML nativo cumple al 100% con peso mínimo.

- **Persistencia Offline-First y Snapshot Histórico**:
  - *Enfoque elegido*: **Room (SQLite)** como única fuente de verdad con `DailyLogEntity` guardando el **snapshot diario** de `estimatedExpenditureKcal`, `errorBudgetKcal`, `minFloorKcal`, `maxCeilingKcal` y `minProteinG`, más almacenamiento de fotos comprimidas en `context.filesDir/pending_meals/` procesadas por `WorkManager` (`NetworkType.CONNECTED`).
  - *Por qué*: Evita que editar el peso objetivo o el perfil meses después altere el déficit histórico de días ya cerrados en la Tabla Maestra, y garantiza que Android no purgue las fotos pendientes (como ocurre en `cacheDir`) antes de recuperar red.
  - *Alternativa considerada*: Calcular el gasto diario dinámicamente en lectura o guardar fotos en `cacheDir`; ambas alternativas corrompen la trazabilidad histórica o pierden registros offline.

---

## 4. Arquitectura Técnica y Estrategia de Datos *(Referencia Técnica)*

### Diagrama de Arquitectura y Flujo de Datos

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                    CAPA UI (Jetpack Compose + Material 3)               │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐  │
│  │ Dashboard │ │  Comida & │ │Recipientes│ │  Pesajes  │ │  Tabla    │  │
│  │ (Caldero) │ │ Simulador │ │(Geometría)│ │& Oráculo  │ │ Maestra   │  │
│  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘  │
│        └─────────────┴─────────────┼─────────────┴─────────────┘        │
│                              ViewModels (StateFlow)                     │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│                     CAPA DOMINIO (Casos de Uso Puros)                   │
│  • ContainerGeometryUseCase      • MetabolicRangeAndDebtUseCase         │
│  • DryWeightAndWaterFilterUseCase • ThermodynamicCalibrationUseCase     │
│  • BiologicalNoteSynthesizer     • DeficitProjectionUseCase             │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
┌────────────────────────────────────▼────────────────────────────────────┐
│                    CAPA DATA (Offline-First Repository)                 │
│  ┌──────────────────────┐  ┌───────────────────┐  ┌──────────────────┐  │
│  │   Room (SQLite SSOT) │  │  DataStore Prefs  │  │  OOXML Excel I/O │  │
│  │  • UserProfile       │  │  • Modelo Gemini  │  │  • SAF Export    │  │
│  │  • DailyLog (Day #)  │  │  • Error Budget   │  │  • SAF Import    │  │
│  │  • MealEntry         │  └───────────────────┘  └──────────────────┘  │
│  │  • Container         │                                               │
│  │  • WeightEntry       │  ┌───────────────────┐  ┌──────────────────┐  │
│  │  • SyncQueueItem     │  │ WorkManager Worker│  │ Firebase AI Logic│  │
│  └──────────────────────┘  │ (NetworkConnected)├─►│ (gemini-3.8-flash│  │
│                            └───────────────────┘  │  JSON Schema)    │  │
│                                                   └──────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### Modelo de Datos y Entidades Room
- **Perfil Biométrico (`UserProfile`)**: Peso inicial/actual, estatura (`cm`), edad, sexo biológico, peso objetivo manual, nivel de actividad cotidiana (multiplicador NEAT), descripción de entrenamiento/fuerza, proteína objetivo (`g/kg`), déficit deseado (`kcal`) y presupuesto de error base (`kcal`, ej. `136`).
- **Jornada Diaria (`DailyLog`)**: `logicalDate` (ISO `YYYY-MM-DD`), `dayNumber` secuencial (`Día 1..N`), snapshot del gasto estimado (`expenditureKcal`), presupuesto de error aplicado (`errorBudgetKcal`), piso anti-catabólico (`floorKcal`), techo de déficit (`ceilingKcal`), bandera `isClosed`, `catabolicDebtCarryoverKcal`, y `biologicalNote` generada al cerrar el día.
- **Registro de Comida (`MealEntry`)**: Relación con `logicalDate`, nombre/descripción, calorías (`Int`), proteína (`Float`), alertas (`List<String>`), resumen breve, referencia opcional a `containerId` + porcentaje de llenado (`25..100`), volumen calculado (`ml`), ruta local interna de foto, estado de sincronización (`SYNCED`, `PENDING_SYNC`, `FAILED`), bandera `isFavorite`, porciones totales (para *Batch Cooking* si se aprueba) y `createdAtEpochMillis`.
- **Recipiente (`Container`)**: Nombre, forma (`CYLINDRICAL`, `RECTANGULAR`, `HEMISPHERICAL`), dimensiones en `cm` (`radiusCm`, `heightCm`, `lengthCm`, `widthCm`), volumen precalculado (`volumeMl`) y bandera `isFavorite`.
- **Pesaje (`WeightEntry`)**: `logicalDate`, peso real (`kg`), `movingAverage7dKg`, `theoreticalDryWeightKg`, `waterRetentionGrams`, bandera `isWhooshDetected`, bandera `outlierConfirmed` (cuando supera $\pm 3.0\,\text{kg}$ en 24h) y timestamp exacto.
- **Cola de Sincronización (`SyncQueueItem`)**: `mealEntryId`, `targetLogicalDate`, ruta de imagen comprimida en `filesDir`, prompt contextual (descripción + recipiente + ml + % llenado + perfil de entrenamiento), reintentos, estado y último error.

### Contrato JSON Obligatorio de Gemini (`responseSchema`)
Se configura mediante `Schema.obj(...)` con `responseMimeType = "application/json"`:
- `calorias_kcal` (`Schema.integer`): Calorías totales estimadas de la porción descrita/fotografiada.
- `proteina_g` (`Schema.double`): Gramos de proteína estimados.
- `etiquetas_alerta` (`Schema.array(Schema.string())`): Etiquetas metabólicas/nutricionales (`"exceso de sodio"`, `"exceso de azúcar"`, `"alta densidad proteica"`, `"fritura"`, etc.).
- `resumen_breve` (`Schema.string`): Síntesis en 1–2 frases con desglose de ingredientes o preparación detectada.

### Mapeo Interactivo de Estado y Lógica de Dominio
- **Geometría Local**: Calcula `volumeMl` en vivo mientras el usuario escribe centímetros (`CYLINDRICAL` = $\pi r^2 h$, `RECTANGULAR` = $l \times a \times h$, `HEMISPHERICAL` = $\frac{2}{3}\pi r^3$) y multiplica por el selector de llenado ($0.25, 0.50, 0.75, 1.0$) para inyectar escala física exacta en el prompt multimodal.
- **Rango Óptimo y Memoria de Deuda Catabólica**: Calcula TDEE (Mifflin-St Jeor + actividad + factor entrenamiento), define el **Piso Anti-Catabólico** (asegurando energía suficiente respecto al peso magro/proteína mínima en `g/kg` y evitando déficits agresivos $> 25\%$–$30\%$ del TDEE o $< 1200/1500\,\text{kcal}$ base según sexo/peso) y el **Techo de Déficit**. Al cerrar un día por debajo del piso, acumula la brecha en `catabolicDebtCarryoverKcal` hacia el día siguiente hasta que un día cierre dentro del rango óptimo.
- **Filtro Hídrico y Auto-Calibración**: En cada nuevo pesaje evalúa la ventana de 24h; si $|\Delta P| \ge 3.0\,\text{kg}$, bloquea el guardado directo tras un diálogo de confirmación explícita. Calcula `theoreticalDryWeight = baseWeight - (cumulativeNetDeficit / 7700.0)` y `waterRetentionGrams = (realWeight - theoreticalDryWeight) * 1000`. Con $\ge 14$ días válidos consecutivos, realiza regresión/balance entre déficit acumulado teórico y pendiente real de media móvil de 7 días para sugerir un ajuste en kcal al gasto diario o al presupuesto de error base.
- **Cola Offline y Resiliencia**: Si `NetworkType` no está conectado o falla la red al confirmar una comida, la entrada se inserta inmediatamente en Room como `PENDING_SYNC` con `logicalDate` congelado y foto copiada a `filesDir/pending_meals/`. `GeminiSyncWorker` procesa los elementos pendientes apenas haya red, actualiza las métricas del `DailyLog` histórico original y elimina el archivo temporal de la cola tras confirmar el guardado.
