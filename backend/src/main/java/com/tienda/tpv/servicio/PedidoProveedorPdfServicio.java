package com.tienda.tpv.servicio;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tienda.tpv.dominio.EstadoPedido;
import com.tienda.tpv.dto.DatosTiendaDTO;
import com.tienda.tpv.dto.LineaPedidoProveedorDTO;
import com.tienda.tpv.dto.PedidoProveedorDTO;
import com.tienda.tpv.dto.ProveedorDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

/** Genera el PDF de un pedido a proveedor, listo para imprimir o adjuntar a un email. */
@Service
public class PedidoProveedorPdfServicio {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Map<EstadoPedido, String> NOMBRES_ESTADO = new EnumMap<>(EstadoPedido.class);

    static {
        NOMBRES_ESTADO.put(EstadoPedido.PENDIENTE, "Pendiente");
        NOMBRES_ESTADO.put(EstadoPedido.RECIBIDO_PARCIAL, "Recibido parcial");
        NOMBRES_ESTADO.put(EstadoPedido.RECIBIDO_COMPLETO, "Completo");
        NOMBRES_ESTADO.put(EstadoPedido.CANCELADO, "Cancelado");
    }

    private final PedidoProveedorServicio pedidoProveedorServicio;
    private final ProveedorServicio proveedorServicio;
    private final DatosTiendaServicio datosTiendaServicio;

    public PedidoProveedorPdfServicio(PedidoProveedorServicio pedidoProveedorServicio,
                                      ProveedorServicio proveedorServicio,
                                      DatosTiendaServicio datosTiendaServicio) {
        this.pedidoProveedorServicio = pedidoProveedorServicio;
        this.proveedorServicio = proveedorServicio;
        this.datosTiendaServicio = datosTiendaServicio;
    }

    public byte[] generar(Long pedidoId) {
        PedidoProveedorDTO pedido = pedidoProveedorServicio.obtener(pedidoId);
        ProveedorDTO proveedor = proveedorServicio.obtener(pedido.proveedorId());
        DatosTiendaDTO tienda = datosTiendaServicio.obtener();

        String html = construirHtml(pedido, proveedor, tienda);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(salida);
            builder.run();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF del pedido #" + pedidoId, e);
        }
        return salida.toByteArray();
    }

    private String construirHtml(PedidoProveedorDTO pedido, ProveedorDTO proveedor, DatosTiendaDTO tienda) {
        StringBuilder filas = new StringBuilder();
        BigDecimal totalEstimado = BigDecimal.ZERO;
        for (LineaPedidoProveedorDTO linea : pedido.lineas()) {
            BigDecimal coste = linea.precioCosteUnitario();
            BigDecimal subtotal = coste != null
                    ? coste.multiply(linea.cantidadPedida()).setScale(2, RoundingMode.HALF_UP)
                    : null;
            if (subtotal != null) {
                totalEstimado = totalEstimado.add(subtotal);
            }
            filas.append("<tr>")
                    .append("<td>").append(escapar(linea.productoNombre())).append("</td>")
                    .append("<td class=\"num\">").append(formatoNumero(linea.cantidadPedida())).append("</td>")
                    .append("<td class=\"num\">").append(coste != null ? formatoEuros(coste) : "&#8212;").append("</td>")
                    .append("<td class=\"num\">").append(subtotal != null ? formatoEuros(subtotal) : "&#8212;").append("</td>")
                    .append("</tr>");
        }

        String notasHtml = pedido.notas() != null && !pedido.notas().isBlank()
                ? "<p class=\"notas\"><strong>Notas:</strong> " + escapar(pedido.notas()) + "</p>"
                : "";

        return "<html><head><meta charset=\"UTF-8\"/><style>"
                + "body { font-family: Helvetica, Arial, sans-serif; font-size: 11px; color: #1e293b; }"
                + "h1 { font-size: 18px; margin-bottom: 2px; }"
                + ".cabecera { width: 100%; margin-bottom: 16px; }"
                + ".caja { display: inline-block; width: 47%; vertical-align: top; border: 1px solid #cbd5e1; border-radius: 6px; padding: 10px 14px; box-sizing: border-box; }"
                + ".caja-para { margin-left: 3%; }"
                + ".caja h2 { font-size: 11px; text-transform: uppercase; color: #64748b; margin: 0 0 6px 0; }"
                + ".caja p { margin: 2px 0; }"
                + "table { width: 100%; border-collapse: collapse; margin-top: 10px; }"
                + "th, td { border-bottom: 1px solid #e2e8f0; padding: 6px 4px; text-align: left; }"
                + "th { color: #64748b; font-size: 10px; text-transform: uppercase; }"
                + ".num { text-align: right; }"
                + ".total { text-align: right; font-weight: bold; font-size: 13px; margin-top: 10px; }"
                + ".notas { margin-top: 16px; font-size: 11px; color: #475569; }"
                + ".pie { margin-top: 30px; font-size: 9px; color: #94a3b8; }"
                + "</style></head><body>"
                + "<h1>Pedido a proveedor #" + pedido.id() + "</h1>"
                + "<p>Fecha: " + pedido.fechaHora().format(FORMATO_FECHA)
                + " &#8212; Estado: " + NOMBRES_ESTADO.get(pedido.estado()) + "</p>"
                + "<div class=\"cabecera\">"
                + "<div class=\"caja\"><h2>De</h2>"
                + "<p><strong>" + escapar(tienda.nombre()) + "</strong></p>"
                + lineaOpcional(tienda.direccion())
                + lineaOpcional(tienda.telefono() != null ? "Tel: " + tienda.telefono() : null)
                + lineaOpcional(tienda.nif() != null ? "NIF: " + tienda.nif() : null)
                + "</div>"
                + "<div class=\"caja caja-para\"><h2>Para</h2>"
                + "<p><strong>" + escapar(proveedor.nombre()) + "</strong></p>"
                + lineaOpcional(proveedor.direccion())
                + lineaOpcional(proveedor.telefono() != null ? "Tel: " + proveedor.telefono() : null)
                + lineaOpcional(proveedor.email())
                + "</div>"
                + "</div>"
                + "<table><thead><tr>"
                + "<th>Producto</th><th class=\"num\">Cantidad</th><th class=\"num\">Coste/ud</th><th class=\"num\">Subtotal</th>"
                + "</tr></thead><tbody>" + filas + "</tbody></table>"
                + "<p class=\"total\">Total estimado: " + formatoEuros(totalEstimado) + "</p>"
                + notasHtml
                + "<p class=\"pie\">Generado por el TPV de " + escapar(tienda.nombre())
                + " &#8212; este documento es un pedido, no una factura.</p>"
                + "</body></html>";
    }

    private String lineaOpcional(String texto) {
        return (texto == null || texto.isBlank()) ? "" : "<p>" + escapar(texto) + "</p>";
    }

    private String formatoNumero(BigDecimal numero) {
        return numero.stripTrailingZeros().toPlainString();
    }

    private String formatoEuros(BigDecimal importe) {
        return importe.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " &#8364;";
    }

    private String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
