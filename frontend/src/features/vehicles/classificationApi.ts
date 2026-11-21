import { postJson } from '../../services/httpClient'

export type VehicleClassification = {
  plate: string
  type: 'CARRO' | 'MOTO'
}

export function classifyVehicle(plate: string): Promise<VehicleClassification> {
  return postJson<VehicleClassification>('/api/v1/vehicles/classify', { plate })
}
