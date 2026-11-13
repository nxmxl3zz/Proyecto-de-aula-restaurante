package restaurante.presentacion;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import restaurante.seguridad.*;

/** Solicita el rol y la contraseña y presenta los errores de autenticación. */
public final class VentanaLogin extends javax.swing.JFrame {
    private final restaurante.controlador.ControladorAcceso autenticacion;
    private final Consumer<Sesion> alEntrar;
    private boolean ingresando;
    public VentanaLogin(restaurante.controlador.ControladorAcceso autenticacion,Consumer<Sesion> alEntrar) {
        this.autenticacion=autenticacion; this.alEntrar=alEntrar; initComponents();
        setTitle("La Doble · Iniciar sesión"); setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImages(Logotipo.iconosVentana());
        marca.setIcon(new Logotipo(48)); marca.setIconTextGap(12);
        getContentPane().setBackground(Tema.FONDO);
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(32,32,32,32));
        portada.setPreferredSize(new Dimension(320,420)); Tema.tarjeta(portada);
        foto.setIcon(Imagenes.cargar("la-doble",275,275)); foto.setHorizontalAlignment(SwingConstants.CENTER);
        columnaFormulario.setOpaque(false); encabezado.setOpaque(false); formulario.setOpaque(false);
        selectorRol.setModel(new DefaultComboBoxModel<>(Rol.values()));
        char eco=campoClave.getEchoChar(); mostrarClave.addActionListener(e -> campoClave.setEchoChar(mostrarClave.isSelected()?(char)0:eco));
        selectorRol.addActionListener(e -> {campoClave.setText("");lblError.setText(" ");});
        lblError.setForeground(new Color(0xF29481)); Tema.principal(btnEntrar);
        getRootPane().setDefaultButton(btnEntrar);
        setMinimumSize(new Dimension(800,540)); setSize(870,600); setLocationRelativeTo(null);
    }
    private void btnEntrarActionPerformed(java.awt.event.ActionEvent evt) {
        if(ingresando) return;
        char[] clave=campoClave.getPassword();
        Sesion sesion;
        try {sesion=autenticacion.autenticar((Rol)selectorRol.getSelectedItem(),clave);}
        catch(java.io.IOException ex) {lblError.setText("No se pudo leer usuarios.txt.");JOptionPane.showMessageDialog(this,ex.getMessage(),"Archivo de usuarios",JOptionPane.ERROR_MESSAGE);return;}
        finally {java.util.Arrays.fill(clave,'\0');campoClave.setText("");}
        if(sesion==null) {lblError.setText("Contraseña incorrecta para el rol seleccionado.");campoClave.requestFocusInWindow();return;}
        ingresando=true;
        try {alEntrar.accept(sesion);dispose();}
        catch(RuntimeException ex) {ingresando=false;lblError.setText("No se pudo abrir la sesión. Revisa los archivos.");JOptionPane.showMessageDialog(this,ex.getMessage(),"No se pudo iniciar",JOptionPane.ERROR_MESSAGE);}
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        getContentPane().setLayout(new java.awt.BorderLayout(32, 0));
        portada = new javax.swing.JPanel();
        portada.setLayout(new java.awt.BorderLayout(0, 12));
        marca = new javax.swing.JLabel();
        marca.setText("La Doble");
        marca.setFont(new java.awt.Font("Serif", 1, 36));
        portada.add(marca, java.awt.BorderLayout.NORTH);
        foto = new javax.swing.JLabel();
        foto.setText("");
        foto.setFont(new java.awt.Font("SansSerif", 0, 14));
        portada.add(foto, java.awt.BorderLayout.CENTER);
        lema = new javax.swing.JLabel();
        lema.setText("Bienvenido al equipo");
        lema.setFont(new java.awt.Font("SansSerif", 1, 23));
        portada.add(lema, java.awt.BorderLayout.SOUTH);
        getContentPane().add(portada, java.awt.BorderLayout.WEST);
        columnaFormulario = new javax.swing.JPanel();
        columnaFormulario.setLayout(new java.awt.BorderLayout(0, 22));
        encabezado = new javax.swing.JPanel();
        encabezado.setLayout(new java.awt.BorderLayout(0, 8));
        titulo = new javax.swing.JLabel();
        titulo.setText("Iniciar sesión");
        titulo.setFont(new java.awt.Font("Serif", 1, 30));
        encabezado.add(titulo, java.awt.BorderLayout.NORTH);
        subtitulo = new javax.swing.JLabel();
        subtitulo.setText("Elige tu rol e ingresa tu contraseña");
        subtitulo.setFont(new java.awt.Font("SansSerif", 0, 14));
        encabezado.add(subtitulo, java.awt.BorderLayout.SOUTH);
        columnaFormulario.add(encabezado, java.awt.BorderLayout.NORTH);
        formulario = new javax.swing.JPanel();
        formulario.setLayout(new java.awt.GridLayout(8, 1, 0, 8));
        lblRol = new javax.swing.JLabel();
        lblRol.setText("Rol");
        lblRol.setFont(new java.awt.Font("SansSerif", 0, 14));
        formulario.add(lblRol);
        selectorRol = new javax.swing.JComboBox<>();
        formulario.add(selectorRol);
        lblClave = new javax.swing.JLabel();
        lblClave.setText("Contraseña");
        lblClave.setFont(new java.awt.Font("SansSerif", 0, 14));
        formulario.add(lblClave);
        campoClave = new javax.swing.JPasswordField();
        campoClave.setColumns(20);
        formulario.add(campoClave);
        mostrarClave = new javax.swing.JCheckBox();
        mostrarClave.setText("Mostrar contraseña");
        formulario.add(mostrarClave);
        lblError = new javax.swing.JLabel();
        lblError.setText(" ");
        lblError.setFont(new java.awt.Font("SansSerif", 0, 14));
        formulario.add(lblError);
        btnEntrar = new javax.swing.JButton();
        btnEntrar.setText("Entrar");
        btnEntrar.addActionListener(this::btnEntrarActionPerformed);
        formulario.add(btnEntrar);
        nota = new javax.swing.JLabel();
        nota.setText("Acceso exclusivo para el personal");
        nota.setFont(new java.awt.Font("SansSerif", 0, 12));
        formulario.add(nota);
        columnaFormulario.add(formulario, java.awt.BorderLayout.CENTER);
        getContentPane().add(columnaFormulario, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel portada;
    private javax.swing.JLabel marca;
    private javax.swing.JLabel foto;
    private javax.swing.JLabel lema;
    private javax.swing.JPanel columnaFormulario;
    private javax.swing.JPanel encabezado;
    private javax.swing.JLabel titulo;
    private javax.swing.JLabel subtitulo;
    private javax.swing.JPanel formulario;
    private javax.swing.JLabel lblRol;
    private javax.swing.JComboBox<restaurante.seguridad.Rol> selectorRol;
    private javax.swing.JLabel lblClave;
    private javax.swing.JPasswordField campoClave;
    private javax.swing.JCheckBox mostrarClave;
    private javax.swing.JLabel lblError;
    private javax.swing.JButton btnEntrar;
    private javax.swing.JLabel nota;
    // End of variables declaration//GEN-END:variables
}
