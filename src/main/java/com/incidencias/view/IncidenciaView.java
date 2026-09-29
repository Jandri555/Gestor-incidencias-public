package com.incidencias.view;

import com.incidencias.Version;
import com.incidencias.service.HistorialService.RegistroIncidencia;
import com.incidencias.utils.FailureSimulator;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

public class IncidenciaView extends JFrame {

    private JTextField txtCurso, txtTaller, txtEquipo, txtAlumno, txtProfesor;
    private JTextArea txtProblema;
    private JLabel lblEstadoCorreo;
    private JButton btnConfigCorreo, btnGenerar, btnAjustes, btnHistorial;

    private JDialog dialogoProgreso;
    private JLabel lblProgresoTexto;

    public IncidenciaView() {
        super("Registro de Incidencias - v" + Version.NUMERO);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 650);

        java.net.URL urlIcono = getClass().getResource("/icon.png");
        if (urlIcono != null) {
            setIconImage(new ImageIcon(urlIcono).getImage());
        } else {
            System.err.println("No se encontró el archivo icon.png en resources.");
        }

        setLayout(new BorderLayout(10, 10));

        JPanel panelForm = new JPanel(new GridLayout(6, 2, 15, 15));
        panelForm.setBorder(BorderFactory.createEmptyBorder(25, 25, 10, 25));

        panelForm.add(new JLabel("Grupo/Curso:"));
        txtCurso = new JTextField();
        panelForm.add(txtCurso);

        panelForm.add(new JLabel("Taller:"));
        txtTaller = new JTextField();
        panelForm.add(txtTaller);

        panelForm.add(new JLabel("Nome Alumna/Alumno:"));
        txtAlumno = new JTextField();
        panelForm.add(txtAlumno);

        panelForm.add(new JLabel("Equipo:"));
        txtEquipo = new JTextField();
        panelForm.add(txtEquipo);

        panelForm.add(new JLabel("Nome Profesor/Profesora:"));
        txtProfesor = new JTextField();
        panelForm.add(txtProfesor);

        panelForm.add(new JLabel("Explicación do problema:"));

        txtProblema = new JTextArea(5, 20);
        txtProblema.setLineWrap(true);
        txtProblema.setWrapStyleWord(true);

        JScrollPane scrollProblema = new JScrollPane(txtProblema);
        scrollProblema.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 25, 10, 25),
                UIManager.getBorder("ScrollPane.border")));

        JPanel panelInferior = new JPanel(new BorderLayout(0, 12));
        panelInferior.setBorder(BorderFactory.createEmptyBorder(10, 25, 20, 25));

        lblEstadoCorreo = new JLabel();
        lblEstadoCorreo.setHorizontalAlignment(SwingConstants.CENTER);
        lblEstadoCorreo.setFont(lblEstadoCorreo.getFont().deriveFont(11f));
        panelInferior.add(lblEstadoCorreo, BorderLayout.NORTH);

        JPanel panelBotonesSecundarios = new JPanel(new GridLayout(1, 3, 10, 0));
        btnHistorial = new JButton("Historial");
        btnAjustes = new JButton("Ajustes");
        btnConfigCorreo = new JButton("Iniciar Sesión");
        panelBotonesSecundarios.add(btnHistorial);
        panelBotonesSecundarios.add(btnAjustes);
        panelBotonesSecundarios.add(btnConfigCorreo);

        btnGenerar = new JButton("Enviar Incidencia");
        btnGenerar.putClientProperty(FlatClientProperties.STYLE, "background: $Button.default.background");

        JPanel panelContenedorBotones = new JPanel(new GridLayout(2, 1, 0, 8));
        panelContenedorBotones.add(panelBotonesSecundarios);
        panelContenedorBotones.add(btnGenerar);

        panelInferior.add(panelContenedorBotones, BorderLayout.CENTER);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(panelForm, BorderLayout.NORTH);
        panelCentro.add(scrollProblema, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }

    public void actualizarEtiquetaEstado(String correoUsuario, String correoPublico, String metodoAutenticacion,
            boolean debugActivado, String debugEmail) {
        String estado;
        if (correoUsuario.isEmpty()) {
            estado = "Sin sesión: pulsa «Iniciar Sesión» para poder enviar";
        } else if ("GOOGLE".equals(metodoAutenticacion)) {
            estado = "Enviando con Google como: " + correoUsuario;
        } else {
            estado = "Enviando como: " + correoUsuario;
        }

        if (debugActivado) {
            estado += " | [MODO DEBUG]";
            lblEstadoCorreo.setForeground(new Color(220, 53, 69));
        } else {
            lblEstadoCorreo.setForeground(UIManager.getColor("Label.disabledForeground"));
        }

        lblEstadoCorreo.setText(estado);
        btnConfigCorreo.setText("Iniciar Sesión");
    }

    public void mostrarProgreso(String texto) {
        SwingUtilities.invokeLater(() -> {
            if (dialogoProgreso == null) {
                dialogoProgreso = new JDialog(this, "Procesando", Dialog.ModalityType.DOCUMENT_MODAL);
                dialogoProgreso.setSize(360, 130);
                dialogoProgreso.setLocationRelativeTo(this);
                dialogoProgreso.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

                JPanel panel = new JPanel(new BorderLayout(10, 15));
                panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

                lblProgresoTexto = new JLabel(texto, SwingConstants.CENTER);
                lblProgresoTexto.setFont(lblProgresoTexto.getFont().deriveFont(Font.BOLD, 12f));
                panel.add(lblProgresoTexto, BorderLayout.NORTH);

                JProgressBar barra = new JProgressBar();
                barra.setIndeterminate(true);
                panel.add(barra, BorderLayout.CENTER);

                dialogoProgreso.add(panel);
            } else {
                lblProgresoTexto.setText(texto);
            }

            if (!dialogoProgreso.isVisible()) {
                new Thread(() -> dialogoProgreso.setVisible(true)).start();
            }
        });
    }

    public void ocultarProgreso() {
        SwingUtilities.invokeLater(() -> {
            if (dialogoProgreso != null) {
                dialogoProgreso.dispose();
                dialogoProgreso = null;
            }
        });
    }

    public void mostrarDialogoHistorial(List<RegistroIncidencia> registros) {
        if (registros == null || registros.isEmpty()) {
            mostrarMensaje("Todavía no hay incidencias registradas en el historial (.sql).",
                    "Historial vacío", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialogo = new JDialog(this, "Historial de Incidencias (historial_incidencias.sql)", Dialog.ModalityType.DOCUMENT_MODAL);
        dialogo.setSize(920, 520);
        dialogo.setLocationRelativeTo(this);
        dialogo.setLayout(new BorderLayout(10, 10));

        String[] columnas = { "Fecha", "Hora", "Curso", "Taller", "Equipo", "Alumno/a", "Profesor/a", "Destinatario" };
        DefaultTableModel modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (RegistroIncidencia r : registros) {
            modeloTabla.addRow(new Object[] {
                    r.fecha, r.hora, r.curso, r.taller, r.equipo, r.alumno, r.profesor, r.destinatario
            });
        }

        JTable tabla = new JTable(modeloTabla) {
            @Override
            public String getToolTipText(MouseEvent e) {
                Point p = e.getPoint();
                int fila = rowAtPoint(p);
                int columna = columnAtPoint(p);
                if (fila >= 0 && columna >= 0) {
                    Object valor = getValueAt(fila, columna);
                    if (valor != null && !valor.toString().isBlank()) {
                        return valor.toString();
                    }
                }
                return super.getToolTipText(e);
            }
        };
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(24);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        tabla.setShowGrid(true);
        tabla.setShowVerticalLines(true);
        tabla.setShowHorizontalLines(true);
        tabla.setIntercellSpacing(new Dimension(1, 1));
        Color colorLinea = UIManager.getColor("Component.borderColor");
        tabla.setGridColor(colorLinea != null ? colorLinea : new Color(100, 100, 100));

        TableColumnModel colModel = tabla.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(85);
        colModel.getColumn(0).setMaxWidth(105);
        colModel.getColumn(1).setPreferredWidth(55);
        colModel.getColumn(1).setMaxWidth(70);
        colModel.getColumn(2).setPreferredWidth(85);
        colModel.getColumn(2).setMaxWidth(130);
        colModel.getColumn(3).setPreferredWidth(55);
        colModel.getColumn(3).setMaxWidth(75);
        colModel.getColumn(4).setPreferredWidth(60);
        colModel.getColumn(4).setMaxWidth(80);
        colModel.getColumn(5).setPreferredWidth(185);
        colModel.getColumn(6).setPreferredWidth(160);
        colModel.getColumn(7).setPreferredWidth(235);

        JScrollPane scrollTabla = new JScrollPane(tabla);

        JTextArea txtDetalleProblema = new JTextArea(5, 40);
        txtDetalleProblema.setEditable(false);
        txtDetalleProblema.setLineWrap(true);
        txtDetalleProblema.setWrapStyleWord(true);
        JScrollPane scrollDetalle = new JScrollPane(txtDetalleProblema);

        JPanel panelDetalle = new JPanel(new BorderLayout(5, 5));
        panelDetalle.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        panelDetalle.add(new JLabel("Explicación del problema (selecciona una fila arriba):"), BorderLayout.NORTH);
        panelDetalle.add(scrollDetalle, BorderLayout.CENTER);

        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int fila = tabla.getSelectedRow();
                if (fila >= 0 && fila < registros.size()) {
                    txtDetalleProblema.setText(registros.get(fila).problema);
                    txtDetalleProblema.setCaretPosition(0);
                }
            }
        });

        int ultimaFila = registros.size() - 1;
        tabla.setRowSelectionInterval(ultimaFila, ultimaFila);
        tabla.scrollRectToVisible(tabla.getCellRect(ultimaFila, 0, true));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollTabla, panelDetalle);
        splitPane.setResizeWeight(0.65);
        splitPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));

        JPanel panelBotonCerrar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dialogo.dispose());
        panelBotonCerrar.add(btnCerrar);

        dialogo.add(splitPane, BorderLayout.CENTER);
        dialogo.add(panelBotonCerrar, BorderLayout.SOUTH);
        dialogo.setVisible(true);
    }

    public ConfigDialogResult mostrarDialogoConfiguracion(String correoActual, char[] passTemporalUI,
            boolean debugActivado,
            String metodoActual,
            String smtpHostActual, String smtpPuertoActual, boolean smtpSSLActual) {

        String estadoActual = correoActual.isEmpty()
                ? "Actualmente: sin sesión iniciada."
                : "GOOGLE".equals(metodoActual)
                        ? "Actualmente: Google (" + correoActual + ")"
                        : "Actualmente: usuario/contraseña (" + correoActual + ")";

        JPanel panelInfo = new JPanel(new BorderLayout(0, 10));
        panelInfo.add(new JLabel(estadoActual), BorderLayout.NORTH);
        panelInfo.add(new JLabel("¿Cómo quieres autenticar el envío de correo?"), BorderLayout.CENTER);

        String[] opciones = { "Iniciar sesión con Google", "Usuario y contraseña", "Borrar credenciales", "Cancelar" };

        int seleccion = mostrarOpcionDialogo(panelInfo,
                "Iniciar Sesión",
                opciones, opciones[1]);

        ConfigDialogResult result = new ConfigDialogResult();
        switch (seleccion) {
            case 0:
                result.accion = ConfigDialogResult.Accion.USAR_GOOGLE;
                return result;
            case 1:
                return mostrarDialogoCredencialesSmtp(correoActual, passTemporalUI,
                        smtpHostActual, smtpPuertoActual, smtpSSLActual);
            case 2:
                result.accion = ConfigDialogResult.Accion.BORRAR;
                return result;
            default:
                result.accion = ConfigDialogResult.Accion.CANCELAR;
                return result;
        }
    }

    private ConfigDialogResult mostrarDialogoCredencialesSmtp(String correoActual, char[] passTemporalUI,
            String smtpHostActual, String smtpPuertoActual, boolean smtpSSLActual) {

        JTextField txtCorreoConfig = new JTextField(correoActual);
        JPasswordField txtPassConfig = new JPasswordField(new String(passTemporalUI));
        txtPassConfig.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");

        JTextField txtSmtpHost = new JTextField(smtpHostActual);
        JTextField txtSmtpPuerto = new JTextField(smtpPuertoActual);
        JCheckBox chkSSL = new JCheckBox("Usar SSL directo (desmarcar para STARTTLS)", smtpSSLActual);
        chkSSL.addActionListener(e -> txtSmtpPuerto.setText(chkSSL.isSelected() ? "465" : "587"));

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Correo propio de envío:"));
        panel.add(txtCorreoConfig);
        panel.add(new JLabel("Contraseña:"));
        panel.add(txtPassConfig);
        panel.add(new JLabel(" "));
        panel.add(new JLabel("Servidor SMTP (ej: smtp.gmail.com):"));
        panel.add(txtSmtpHost);
        panel.add(new JLabel("Puerto (465 SSL, 587 STARTTLS...):"));
        panel.add(txtSmtpPuerto);
        panel.add(chkSSL);

        String[] opciones = { "Guardar", "Cancelar" };
        int seleccion = mostrarOpcionDialogo(panel,
                "Usuario y contraseña",
                opciones, opciones[0]);

        ConfigDialogResult result = new ConfigDialogResult();
        if (seleccion == 0) {
            result.accion = ConfigDialogResult.Accion.GUARDAR_PASSWORD;
            result.correo = txtCorreoConfig.getText().trim();
            result.password = txtPassConfig.getPassword();
            result.smtpHost = txtSmtpHost.getText().trim();
            result.smtpPuerto = txtSmtpPuerto.getText().trim();
            result.smtpSSL = chkSSL.isSelected();
        } else {
            result.accion = ConfigDialogResult.Accion.CANCELAR;
        }
        txtPassConfig.setText("");
        return result;
    }

    public SmtpConfigResult mostrarDialogoAjustesSmtp(String hostActual, String puertoActual, boolean sslActual,
            boolean modoDebugActual, String debugEmailActual) {

        JTextField txtHost = new JTextField(hostActual);
        JTextField txtPuerto = new JTextField(puertoActual);
        JCheckBox chkSSL = new JCheckBox("Usar SSL directo (desmarcar para STARTTLS)", sslActual);
        chkSSL.addActionListener(e -> txtPuerto.setText(chkSSL.isSelected() ? "465" : "587"));
        JTextField txtDebugEmail = null;
        JButton btnPanelTest = null;

        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.add(new JLabel("Servidor SMTP por defecto:"));
        panel.add(txtHost);
        panel.add(new JLabel("Puerto:"));
        panel.add(txtPuerto);
        panel.add(chkSSL);

        if (modoDebugActual) {
            txtDebugEmail = new JTextField(debugEmailActual);
            panel.add(new JLabel(" "));
            panel.add(new JLabel("Correo de destino de prueba (Modo Debug activo):"));
            panel.add(txtDebugEmail);

            btnPanelTest = new JButton("🛠️ Abrir Panel de Emulación de Fallos");
            btnPanelTest.addActionListener(e -> mostrarDialogoPanelTest());
            panel.add(new JLabel(" "));
            panel.add(btnPanelTest);
        }

        String[] opciones = { "Guardar", "Cancelar" };
        int seleccion = mostrarOpcionDialogo(panel,
                "Ajustes",
                opciones, opciones[0]);

        SmtpConfigResult result = new SmtpConfigResult();
        result.guardado = (seleccion == 0);
        result.host = txtHost.getText().trim();
        result.puerto = txtPuerto.getText().trim();
        result.ssl = chkSSL.isSelected();
        result.debugEmail = (modoDebugActual && txtDebugEmail != null)
                ? txtDebugEmail.getText().trim()
                : debugEmailActual;
        return result;
    }

    private void mostrarDialogoPanelTest() {
        JCheckBox chkLibreOffice = new JCheckBox("Simular que falta LibreOffice",
                FailureSimulator.isSimularFaltaLibreOffice());
        JCheckBox chkInternet = new JCheckBox("Simular fallo de conexión / Red",
                FailureSimulator.isSimularSinInternet());

        JPanel panelTest = new JPanel(new GridLayout(0, 1, 8, 8));
        panelTest.add(new JLabel("<html><b>Selecciona los fallos que deseas emular:</b></html>"));
        panelTest.add(chkLibreOffice);
        panelTest.add(chkInternet);

        int res = mostrarConfirmDialogo(panelTest, "Panel de Emulación de Fallos",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

        if (res == JOptionPane.OK_OPTION) {
            FailureSimulator.setSimularFaltaLibreOffice(chkLibreOffice.isSelected());
            FailureSimulator.setSimularSinInternet(chkInternet.isSelected());
        }
    }

    private int mostrarOpcionDialogo(Object mensaje, String titulo, Object[] opciones, Object valorInicial) {
        JOptionPane pane = new JOptionPane(
                mensaje,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                opciones,
                valorInicial);

        JDialog dialogo = pane.createDialog(this, titulo);
        dialogo.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        dialogo.setResizable(false);
        dialogo.setVisible(true);

        Object valor = pane.getValue();
        if (valor == null) {
            return JOptionPane.CLOSED_OPTION;
        }

        for (int i = 0; i < opciones.length; i++) {
            if (valor.equals(opciones[i])) {
                return i;
            }
        }
        return JOptionPane.CLOSED_OPTION;
    }

    private int mostrarConfirmDialogo(Object mensaje, String titulo, int opciones, int tipo) {
        JOptionPane pane = new JOptionPane(
                mensaje,
                tipo,
                opciones);

        JDialog dialogo = pane.createDialog(this, titulo);
        dialogo.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        dialogo.setResizable(false);
        dialogo.setVisible(true);

        Object valor = pane.getValue();
        return valor instanceof Integer ? (Integer) valor : JOptionPane.CLOSED_OPTION;
    }

    public void mostrarMensaje(String mensaje, String titulo, int tipo) {
        JOptionPane pane = new JOptionPane(mensaje, tipo, JOptionPane.DEFAULT_OPTION);
        JDialog dialogo = pane.createDialog(this, titulo);
        dialogo.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        dialogo.setResizable(false);
        dialogo.setVisible(true);
    }

    public void enfocarCajaProblema() {
        txtProblema.requestFocusInWindow();
    }

    public JButton getBtnConfigCorreo() {
        return btnConfigCorreo;
    }

    public JButton getBtnGenerar() {
        return btnGenerar;
    }

    public JButton getBtnAjustes() {
        return btnAjustes;
    }

    public JButton getBtnHistorial() {
        return btnHistorial;
    }

    public String getCurso() {
        return txtCurso.getText();
    }

    public String getTaller() {
        return txtTaller.getText();
    }

    public String getEquipo() {
        return txtEquipo.getText();
    }

    public String getAlumno() {
        return txtAlumno.getText();
    }

    public String getProfesor() {
        return txtProfesor.getText();
    }

    public String getProblema() {
        return txtProblema.getText();
    }

    public void setCurso(String txt) {
        txtCurso.setText(txt);
    }

    public void setTaller(String txt) {
        txtTaller.setText(txt);
    }

    public void setEquipo(String txt) {
        txtEquipo.setText(txt);
    }

    public void setAlumno(String txt) {
        txtAlumno.setText(txt);
    }
}
