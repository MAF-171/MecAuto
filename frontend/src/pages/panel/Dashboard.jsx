import { useCarga } from '../../lib/hooks';
import { useAuth } from '../../lib/auth.jsx';
import { Cargando, ErrorCaja, PaginaCab, Insignia, Tabla } from '../../components/ui.jsx';
import { Link } from '../../lib/router.jsx';
import { soles, etiqueta, fechaHora } from '../../lib/format';

export default function Dashboard() {
  const { usuario } = useAuth();
  const { datos: d, cargando, error, recargar } = useCarga('/dashboard');
  if (cargando && !d) return <Cargando />;
  if (error) return <ErrorCaja mensaje={error} onReintentar={recargar} />;
  if (!d) return null;
  const admin = usuario.rol === 'ADMINISTRADOR';
  const max = Math.max(1, ...(d.ingresosPorSemana || []).map((p) => Number(p.total)));
  return (
    <>
      <PaginaCab titulo="Panel de control" subtitulo={'Hola, ' + usuario.nombre + '. Este es el estado actual del taller.'} />
      <div className="kpis">
        {d.totalClientes != null && <Kpi et="Clientes activos" v={d.totalClientes} />}
        {d.totalVehiculos != null && <Kpi et="Vehículos registrados" v={d.totalVehiculos} />}
        {d.citasHoy != null && <Kpi et="Citas de hoy" v={d.citasHoy} tono="azul" />}
        <Kpi et="Órdenes pendientes" v={d.ordenesPendientes} tono="amarillo" />
        <Kpi et="Órdenes en proceso" v={d.ordenesEnProceso} tono="rojo" />
        <Kpi et="Órdenes finalizadas" v={d.ordenesFinalizadas} tono="verde" />
        {d.vehiculosEnTaller != null && <Kpi et="Vehículos en taller" v={d.vehiculosEnTaller} />}
        {d.repuestosStockBajo != null && <Kpi et="Repuestos con stock bajo" v={d.repuestosStockBajo} tono={d.repuestosStockBajo ? 'rojo' : 'verde'} />}
        {admin && d.ingresosMes != null && <Kpi et="Ingresos del mes (comprobantes emitidos)" v={soles(d.ingresosMes)} tono="verde" />}
      </div>

      <div className="dos-col">
        <section className="tarjeta">
          <h2>Órdenes por estado</h2>
          <ul className="estado-lista">
            {Object.entries(d.ordenesPorEstado || {}).map(([k, v]) => <li key={k}><Insignia estado={k} /><strong>{v}</strong></li>)}
          </ul>
        </section>
        {admin && (
          <section className="tarjeta">
            <h2>Ingresos por semana</h2>
            {(d.ingresosPorSemana || []).length === 0 ? <p className="estado-vacio">Aún no hay comprobantes emitidos en el periodo.</p> : (
              <div className="barras" role="img" aria-label="Ingresos por semana">
                {d.ingresosPorSemana.map((p) => (
                  <div className="col" key={p.etiqueta} title={soles(p.total)}>
                    <span>{soles(p.total)}</span><i style={{ height: Math.max(2, (Number(p.total) / max) * 100) + '%' }} /><span>{p.etiqueta}</span>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}
      </div>

      {d.alertasStock && d.alertasStock.length > 0 && (
        <section className="tarjeta">
          <h2>Alertas de stock bajo</h2>
          <Tabla columnas={[
            { titulo: 'Código', ver: (r) => r.codigo }, { titulo: 'Repuesto', ver: (r) => r.nombre },
            { titulo: 'Stock', clase: 'num', ver: (r) => r.stockActual }, { titulo: 'Mínimo', clase: 'num', ver: (r) => r.stockMinimo },
          ]} filas={d.alertasStock} />
          <p><Link to="/panel/inventario">Ir al inventario →</Link></p>
        </section>
      )}

      <section className="tarjeta">
        <h2>Órdenes recientes</h2>
        <Tabla vacio="Todavía no hay órdenes."
          columnas={[
            { titulo: 'Código', ver: (o) => <Link to={'/panel/ordenes/' + o.id}>{o.codigo}</Link> },
            { titulo: 'Cliente', ver: (o) => o.clienteNombre }, { titulo: 'Vehículo', ver: (o) => o.placa + ' · ' + o.vehiculo },
            { titulo: 'Estado', ver: (o) => <Insignia estado={o.estado} /> }, { titulo: 'Ingreso', ver: (o) => fechaHora(o.fechaIngreso) },
          ]} filas={d.ordenesRecientes} />
      </section>
    </>
  );
}

const Kpi = ({ et, v, tono }) => <div className={'kpi ' + (tono || '')}><div className="valor">{v}</div><div className="et">{et}</div></div>;
