# SoundPod — Portadas y fondo personalizables (fork)

Fork de [arunnechully/SoundPod](https://github.com/arunnechully/SoundPod)
(licencia **GPL-3.0**). Agrega, **100% desde dentro de la app**, la posibilidad
de usar fotos propias como:

1. **Portada de una canción** — menú `…` de cualquier canción → *Change cover*
   (*Cambiar portada*). Se abre el selector de fotos del teléfono, eliges la
   imagen y reemplaza la portada de YouTube Music en el reproductor y en las
   listas. *Reset cover* (*Restablecer portada*) vuelve a la original.
2. **Portada de un álbum** — pantalla del álbum → menú `⋮` → *Change cover*.
   Aplica también a las canciones de ese álbum que no tengan portada propia
   (la portada de la canción gana sobre la del álbum).
3. **Portada de un artista** — se muestra en la cabecera de su pantalla
   (cambiarla: próximamente desde su menú).
4. **Fondo del reproductor con tu foto** — *Ajustes → Fondo* → *My photo*
   (*Mi foto*): eliges la imagen y queda como fondo a pantalla completa.
   Los colores de la interfaz se extraen de tu foto, igual que antes se
   extraían de la portada. *Remove photo* la quita.

## Cómo funciona por dentro

| Pieza | Archivo |
|---|---|
| Tabla `CustomArtwork(itemKey, fileName, updatedAt)` + migración Room 28→29 | `customart/CustomArtwork.kt`, `Database.kt` |
| Copia de la foto a `filesDir/custom_artwork/` (sobrevive si borras la original de la galería) | `CustomArtworkStore` |
| Selector de fotos en la app (`GetContent("image/*")`, sin permisos) | `customart/CustomArtworkPicker.kt` |
| Punto único de decisión: foto propia o portada original | `customart/CustomAwareImage.kt` (`effectiveArtwork`) |
| Reproductor (portada grande) | `ui/screens/player/SharedThumbnail.kt` |
| Fondo del reproductor | `ui/appearance/PlayerBackground.kt` + `ui/screens/settings/subscreen/BackgroundSettings.kt` |
| Listas de canciones/álbumes, menús `…` | `ui/items/ItemContainer.kt`, `ui/items/SongItem.kt`, `ui/items/AlbumItem.kt`, `ui/components/MediaItemMenu.kt` |
| Cabeceras de álbum/artista | `ui/components/ParallaxScreenLayout.kt`, `AdaptiveThumbnail.kt`, `ui/screens/album/AlbumScreen.kt`, `ui/screens/artist/ArtistScreen.kt` |

La clave que vincula foto ↔ canción/álbum/artista es el ID estable de
YouTube Music (`videoId` / `browseId`), así que tu foto sigue ahí aunque
cierres y abras la app. Para canciones locales (archivos de tu teléfono) la
clave es su `content://`, también estable.

## Compilar

```bash
# Requisitos: Android Studio (o JDK 17+) + Android SDK 37
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

> **Nota sobre el esquema Room:** en este fork `exportSchema` quedó en
> `false` para que el `29.json` de referencia no rompa la primera
> compilación por diferencia de *identity hash*. Después de la primera
> compilación exitosa, vuelve a poner `exportSchema = true` y confirma el
> `29.json` que genere Room.

> **Nota de licencia:** al ser GPL-3.0, si compartes el APK modificado debes
> publicar también el código fuente modificado con la misma licencia.

## Alcance de esta primera versión

- ✅ Portadas por canción y por álbum, fondo "Mi foto".
- ✅ Reset a la portada original en cualquier momento.
- ⏳ Pendiente (fácil de agregar siguiendo el mismo patrón): cambiar portada
  de artista y de playlist desde su propio menú, y portada en widgets /
  notificaciones.
