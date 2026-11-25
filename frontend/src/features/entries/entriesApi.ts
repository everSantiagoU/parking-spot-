import { postJson } from '../../services/httpClient'

type EntryResponse = {
  resultado: string
  barreraAbierta: boolean
  plate: string
}

export async function registerManualEntry(plate: string) {
  const entry = await postJson<EntryResponse>('/api/v1/entries/manual', { plate })
  if (entry?.resultado !== 'AUTORIZADO' || entry.barreraAbierta !== true || !entry.plate) {
    throw new Error('El servidor no confirmó el registro del ingreso.')
  }
  return entry.plate
}
