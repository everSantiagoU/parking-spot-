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
})
