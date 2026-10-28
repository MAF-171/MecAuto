package pe.mecauto.util;

/**
 * Normaliza números de celular al formato internacional (+51987654321) para guardarlos y buscarlos igual
 * sin importar cómo los escriba la persona ("987 654 321", "+51 987-654-321", "51987654321").
 */
public final class TelefonoUtil {

    private TelefonoUtil() {
    }

    /** Devuelve el número como "+" seguido de 8 a 15 dígitos, o null si no parece un celular válido. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.length() == 9 && digitos.startsWith("9")) {
            digitos = "51" + digitos; // celular peruano escrito sin código de país
        }
        return digitos.length() >= 8 && digitos.length() <= 15 ? "+" + digitos : null;
    }
}
