# Guía de contribución y versionamiento

## Principios
- **TDD:** primero la prueba que falla, luego el mínimo código que la hace pasar, luego el refactor.
- **SOLID, arquitectura limpia, DRY, KISS y YAGNI.** Ver [docs/decisiones.md](docs/decisiones.md).
- **Calidad basada en riesgo.** Ver [docs/qa/estrategia-pruebas.md](docs/qa/estrategia-pruebas.md).

## Commits: Conventional Commits
Formato: `tipo(ámbito opcional): descripción en español, en minúscula y en imperativo`

| Tipo | Uso | Ejemplo |
|---|---|---|
| `feat` | Funcionalidad nueva | `feat(domain): validar que la orden tenga al menos un producto` |
| `test` | Agregar o mejorar pruebas | `test(domain): RN-01 rechazar orden sin productos` |
| `refactor` | Cambio interno sin cambiar el comportamiento | `refactor(domain): centralizar transiciones en OrderStatus` |
| `fix` | Corrección de un defecto | `fix(api): responder 404 cuando la orden no existe` |
| `docs` | Documentación | `docs(qa): agregar matriz de trazabilidad RN a pruebas` |
| `build` | Maven, Docker, dependencias | `build: configurar JaCoCo con umbral de 80% en líneas y ramas` |
| `ci` | Pipeline | `ci: agregar pipeline de build en GitHub Actions` |
| `chore` | Mantenimiento sin impacto funcional | `chore: estructurar paquetes según arquitectura limpia` |

**Ámbitos:** `domain`, `application`, `infrastructure`, `api`, `qa`, `arch`, `observability`, `e2e`.

### Evidencia TDD en el historial
Cada regla de negocio deja en el historial la secuencia completa:

```
test(domain): RN-05 rechazar envío de orden no pagada      ← rojo
feat(domain): impedir enviar una orden sin pagar           ← verde
refactor(domain): centralizar transiciones en OrderStatus  ← refactor (si aplica)
```

Los commits `test` pueden dejar el build en rojo dentro de la rama. El CI valida solo el último commit del PR y los pushes a `main`.

## Ramas
Una rama por fase, creada desde `main` actualizado:

| Prefijo | Uso | Ejemplo |
|---|---|---|
| `docs/` | Documentación | `docs/fase-0-analisis-diseno` |
| `chore/` | Estructura y mantenimiento | `chore/fase-1-esqueleto` |
| `feature/` | Funcionalidad | `feature/fase-2-dominio` |
| `test/` | Pruebas | `test/fase-4-karate` |
| `build/` | Empaquetado | `build/fase-5-docker` |
| `ci/` | Pipeline | `ci/fase-6-pipeline` |

## Pull requests
1. El título sigue Conventional Commits.
2. La descripción incluye qué cambia, por qué, cómo se probó y qué reglas o riesgos cubre (enlace a la [matriz de trazabilidad](docs/qa/trazabilidad.md)).
3. El CI debe estar en verde: build, pruebas, cobertura, mutación y Karate según la fase.
4. Se hace merge con **"Create a merge commit"**. No se usa squash ni rebase merge, para conservar la secuencia TDD como evidencia.
5. La rama se borra después del merge.

`main` está protegida: no acepta push directo ni force push, y exige PR con los checks obligatorios.

## Versiones y tags
Se usa versionamiento semántico. Cada fase cerrada genera un tag anotado sobre `main`:

| Tag | Hito |
|---|---|
| `v0.1.0` | Fase 0: análisis y diseño |
| `v0.2.0` | Fase 1: esqueleto y CI mínimo |
| `v0.3.0` | Fase 2: dominio con TDD |
| `v0.4.0` | Fase 3: persistencia y API |
| `v0.5.0` | Fase 4: pruebas de aceptación con Karate |
| `v0.6.0` | Fase 5: Docker |
| `v0.7.0` | Fase 6: pipeline completo y quality gates |
| `v1.0.0` | Fase 7: entrega final (GitHub Release) |

```powershell
git checkout main; git pull
git tag -a v0.1.0 -m "Fase 0: análisis y diseño"
git push origin v0.1.0
```

## Uso de IA
Este proyecto usa asistentes de IA (Claude Code) para acelerar el trabajo. Toda contribución generada con IA:
- pasa por PR con revisión humana antes del merge;
- debe cumplir los mismos quality gates que el código escrito a mano;
- queda identificada en el commit con `Co-Authored-By`.
