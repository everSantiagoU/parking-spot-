import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { AppRouter } from '../app/router/AppRouter'

describe('ruta inicial', () => {
  it('muestra el título de Parking Spot en /', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter initialEntries={['/']}>
        <AppRouter />
      </MemoryRouter>,
    )

    expect(html).toContain('Parking Spot')
  })

  it('muestra el formulario de clasificación en /vehicles/classify', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter initialEntries={['/vehicles/classify']}>
        <AppRouter />
      </MemoryRouter>,
    )

    expect(html).toContain('<form')
    expect(html).toContain('name="plate"')
    expect(html).toContain('Clasificar vehículo</button>')
  })

  it('muestra el panel de operador en /operator con sus módulos', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter initialEntries={['/operator']}>
        <AppRouter />
      </MemoryRouter>,
    )

    expect(html).toContain('Panel de operador')
    expect(html).toContain('Carril de salida')
    expect(html).toContain('href="/vehicles/classify"')
  })

  it('muestra el panel de administración en /admin con sus módulos', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter initialEntries={['/admin']}>
        <AppRouter />
      </MemoryRouter>,
    )

    expect(html).toContain('Panel de administración')
    expect(html).toContain('Tarifas y franjas')
  })

  it('la página de inicio enlaza a los paneles de operador y administrador', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter initialEntries={['/']}>
        <AppRouter />
      </MemoryRouter>,
    )

    expect(html).toContain('href="/operator"')
    expect(html).toContain('href="/admin"')
  })
})
