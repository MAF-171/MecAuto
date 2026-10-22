import { useState } from 'react';
import { api } from '../../lib/api';
import { etiqueta, fecha } from '../../lib/format';

const ETAPAS = ['RECIBIDA', 'EN_DIAGNOSTICO', 'EN_REPARACION', 'COMPLETADA', 'ENTREGADA'];

/**
 * Consulta pública del estado de una orden: código + últimos 3 caracteres de la placa.
 * Muestra un tablero por etapas con la orden del cliente resaltada. Nunca muestra datos de otros clientes.
 */
export default function SeguimientoOrden() {
  const [codigo, setCodigo] = useState('');
  const [placa, setPlaca] = useState('');
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState('');
  const [orden, setOrden] = useState(null);

  async function consultar(e) {
    e.preventDefault();
    setError(''); setOrden(null);
    if (!codigo.trim() || placa.trim().length < 3) { setError('Escribe el código de tu orden y los últimos 3 caracteres de la placa.'); return; }
    setCargando(true);
    try {
      setOrden(await api.post('/public/ordenes/consulta', { codigo: codigo.trim(), placa: placa.trim() }));
    } catch (err) {
      setError(err.message || 'No pudimos consultar tu orden. Inténtalo de nuevo.');
    } finally { setCargando(false); }
  }
  const limpiar = () => { setOrden(null); setError(''); setCodigo(''); setPlaca(''); };
  const actual = orden ? ETAPAS.indexOf(orden.estado) : -1;

  return (
    <section className="seccion revelar" id="seguimiento" style={{ paddingTop: 0 }}>
      <h2>Consulta el avance de tu <b>orden</b></h2>
      <p className="lead">Escribe el código que te dio el taller y los últimos 3 caracteres de la placa. Solo mostramos la etapa en la que está tu vehículo, nunca datos personales.</p>

      <form className="sg-form" onSubmit={consultar} noValidate>
        <label className="campo"><span className="campo-et">Código de orden</span>
          <input value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="OT-0002" maxLength={12} autoComplete="off" style={{ textTransform: 'uppercase' }} />
        </label>
        <label className="campo"><span className="campo-et">Últimos 3 de la placa</span>
          <input value={placa} onChange={(e) => setPlaca(e.target.value)} placeholder="029" maxLength={10} autoComplete="off" style={{ textTransform: 'uppercase' }} />
        </label>
        <div className="sg-botones">
          <button className="btn btn-oscuro" disabled={cargando}>{cargando ? 'Consultando…' : 'Consultar'}</button>
          {(orden || error) && <button type="button" className="btn btn-sec" onClick={limpiar}>Limpiar</button>}
        </div>
      </form>

      {error && <div className="caja-error" role="alert">{error}</div>}

      {orden && orden.estado === 'CANCELADA' && (
        <div className="caja-info" role="status">La orden <strong>{orden.codigo}</strong> fue cancelada. Si tienes dudas, comunícate con el taller.</div>
      )}

      {orden && orden.estado !== 'CANCELADA' && (
        <div className="sg-tablero" role="status" aria-live="polite">
          {ETAPAS.map((est, i) => {
            const aqui = i === actual;
            return (
              <div key={est} className={'sg-col' + (aqui ? ' aqui' : '') + (i < actual ? ' hecha' : '')}>
                <div className="sg-cab"><span>{etiqueta(est)}</span><b>{aqui ? '1' : i < actual ? '✓' : '–'}</b></div>
                <div className="sg-cuerpo">
                  {aqui ? (
                    <div className="sg-ficha">
                      <div className="sg-fila"><strong>{orden.codigo}</strong><span className="sg-tu">Tu orden</span></div>
                      <img src="/img/auto-rojo.jpg" alt="Tu vehículo" />
                      <div className="sg-datos">
                        {orden.servicio && <strong>{orden.servicio}</strong>}
                        <span>Placa {orden.placa}</span>
                        <span>Ingresó el {fecha(orden.fechaIngreso)}</span>
                        {orden.fechaEntregaEstimada && <span>Entrega estimada: {fecha(orden.fechaEntregaEstimada)}</span>}
                        <span className="sg-etapa">Etapa {i + 1} de 5</span>
                      </div>
                    </div>
                  ) : <div className="sg-vacio">{i < actual ? 'Etapa completada' : 'Pendiente'}</div>}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
