export type VehicleClassification = {
  plate: string
  type: 'CARRO' | 'MOTO'
}

type ApiError = { code?: string; message?: string }

export async function classifyVehicle(plate: string): Promise<VehicleClassification> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
  const response = await fetch(`${baseUrl.replace(/\/$/, '')}/api/v1/vehicles/classify`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ plate }),
  })

  if (!response.ok) {
    const error = (await response.json().catch(() => ({}))) as ApiError
    throw new Error(error.message ?? 'No se pudo clasificar la placa.')
  }

  return (await response.json()) as VehicleClassification
}
