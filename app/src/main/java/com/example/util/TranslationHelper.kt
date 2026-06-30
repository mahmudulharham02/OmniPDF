package com.example.util

object TranslationHelper {
    private val translations = mapOf(
        "en" to mapOf(
            "app_name" to "OmniPDF",
            "greeting" to "Welcome to OmniPDF",
            "subtitle" to "Dynamic on-device PDF utilities",
            "settings" to "Settings",
            "appearance" to "Appearance Settings",
            "theme_label" to "Theme Option",
            "theme_light" to "Classic Light (Soft Gray/Charcoal)",
            "theme_dark" to "Pitch Dark (Absolute OLED Black)",
            "typography_label" to "Typography Style",
            "typo_sans" to "Modern Sans-Serif",
            "typo_mono" to "Technical Monospaced",
            "language_label" to "App Language",
            "lang_en" to "English (en)",
            "lang_es" to "Spanish (es)",
            "lang_fr" to "French (fr)",
            "save" to "Save",
            "back" to "Back",
            "close" to "Close",
            "status_ready" to "Ready",
            "status_processing" to "Processing on-device...",
            "status_success" to "Operation completed successfully!",
            "status_error" to "Error occurred:",
            
            // Tool 1: Scanner
            "tool_scanner_title" to "Scan Document",
            "tool_scanner_desc" to "Convert physical papers into high-fidelity PDFs via camera.",
            "tool_scanner_snap" to "Snap Page",
            "tool_scanner_simulate" to "Simulate Snap (High-Fi PDF)",
            "tool_scanner_compile" to "Compile Pages to PDF",
            "tool_scanner_scanned_pages" to "Captured Pages:",
            "tool_scanner_empty" to "No pages captured yet. Click below to snap a page.",

            // Tool 2: PDF Viewer
            "tool_viewer_title" to "PDF Viewer",
            "tool_viewer_desc" to "Crisp, local document browser with pinch-to-zoom.",
            "tool_viewer_select" to "Select Local PDF File",
            "tool_viewer_no_file" to "No PDF document selected yet.",
            "tool_viewer_page_index" to "Page:",
            "tool_viewer_zoom_tip" to "Use pinch gestures to zoom the page bitmap.",

            // Tool 3: Split PDF
            "tool_split_title" to "Split PDF",
            "tool_split_desc" to "Extract specific segments or custom page ranges.",
            "tool_split_range" to "Page Range (e.g. 1-2, 4):",
            "tool_split_button" to "Split & Export Segments",
            "tool_split_placeholder" to "e.g., 1-2, 5",

            // Tool 4: Merge PDF
            "tool_merge_title" to "Merge PDF",
            "tool_merge_desc" to "Combine multiple local documents into a single PDF.",
            "tool_merge_add" to "Add PDF File",
            "tool_merge_queue" to "Document Compilation Queue:",
            "tool_merge_button" to "Compile & Merge PDFs",
            "tool_merge_empty" to "Queue is empty. Select files to merge.",

            // Tool 5: Sign PDF
            "tool_sign_title" to "Sign PDF",
            "tool_sign_desc" to "Authenticate files with hand-drawn signature overlays.",
            "tool_sign_pad" to "Digital Signature Pad (Touch/Stylus)",
            "tool_sign_clear" to "Clear Pad",
            "tool_sign_burn" to "Sign & Burn to Document",
            "tool_sign_tip" to "Draw your signature below and tap to embed in the PDF.",

            // Tool 6: Encrypt PDF
            "tool_encrypt_title" to "Encrypt PDF",
            "tool_encrypt_desc" to "Lock confidential PDFs with strong AES-256 secure locker.",
            "tool_encrypt_password" to "Encryption Password:",
            "tool_encrypt_button" to "Secure & Encrypt File",
            "tool_encrypt_secured" to "File successfully placed in Secure Locker!",

            // Tool 7: Watermark PDF
            "tool_watermark_title" to "Watermark PDF",
            "tool_watermark_desc" to "Superimpose custom overlay text or draft stamps.",
            "tool_watermark_text" to "Watermark Stamp Text:",
            "tool_watermark_placeholder" to "CONFIDENTIAL",
            "tool_watermark_opacity" to "Opacity:",
            "tool_watermark_rotation" to "Rotation angle:",
            "tool_watermark_button" to "Stamp & Overlay PDF",

            // Tool 8: PDF to Image
            "tool_pdftoimg_title" to "PDF to Image",
            "tool_pdftoimg_desc" to "Export pages directly into high-quality JPEG photos.",
            "tool_pdftoimg_button" to "Export PDF Pages to Images",

            // Tool 9: Extract Images
            "tool_extract_title" to "Extract Images",
            "tool_extract_desc" to "Rip and isolate embedded illustrations from documents.",
            "tool_extract_button" to "Rip Graphics & Extract Images",
            
            // Permissions
            "perm_title" to "Storage Access Required",
            "perm_rationale" to "OmniPDF processes all documents entirely on-device to protect your privacy. This requires storage permission to read/write local PDFs.",
            "perm_grant" to "Grant Permission",
            "perm_manual" to "Open App Settings",
            "perm_alert" to "Permissions are required to use this utility suite offline."
        ),
        "es" to mapOf(
            "app_name" to "OmniPDF",
            "greeting" to "Bienvenido a OmniPDF",
            "subtitle" to "Utilidades PDF dinámicas en el dispositivo",
            "settings" to "Ajustes",
            "appearance" to "Ajustes de Apariencia",
            "theme_label" to "Opción de Tema",
            "theme_light" to "Classic Light (Gris Suave/Carbón)",
            "theme_dark" to "Pitch Dark (Negro OLED Absoluto)",
            "typography_label" to "Estilo de Tipografía",
            "typo_sans" to "Sans-Serif Moderna",
            "typo_mono" to "Monoespaciado Técnico",
            "language_label" to "Idioma de la Aplicación",
            "lang_en" to "Inglés (en)",
            "lang_es" to "Español (es)",
            "lang_fr" to "Francés (fr)",
            "save" to "Guardar",
            "back" to "Volver",
            "close" to "Cerrar",
            "status_ready" to "Listo",
            "status_processing" to "Procesando en el dispositivo...",
            "status_success" to "¡Operación completada con éxito!",
            "status_error" to "Ocurrió un error:",

            // Tool 1: Scanner
            "tool_scanner_title" to "Escanear Documento",
            "tool_scanner_desc" to "Convierta papeles físicos en PDF de alta fidelidad con la cámara.",
            "tool_scanner_snap" to "Capturar Página",
            "tool_scanner_simulate" to "Simular Captura (PDF de alta fidelidad)",
            "tool_scanner_compile" to "Compilar Páginas a PDF",
            "tool_scanner_scanned_pages" to "Páginas Capturadas:",
            "tool_scanner_empty" to "No se han capturado páginas todavía. Toque para capturar.",

            // Tool 2: PDF Viewer
            "tool_viewer_title" to "Visor de PDF",
            "tool_viewer_desc" to "Explorador de documentos locales con zoom táctil.",
            "tool_viewer_select" to "Seleccionar Archivo PDF Local",
            "tool_viewer_no_file" to "No se ha seleccionado ningún documento PDF.",
            "tool_viewer_page_index" to "Página:",
            "tool_viewer_zoom_tip" to "Pellizque para ampliar el mapa de bits de la página.",

            // Tool 3: Split PDF
            "tool_split_title" to "Dividir PDF",
            "tool_split_desc" to "Extraer segmentos específicos o rangos de páginas.",
            "tool_split_range" to "Rango de Páginas (ej. 1-2, 4):",
            "tool_split_button" to "Dividir y Exportar Segmentos",
            "tool_split_placeholder" to "ej., 1-2, 5",

            // Tool 4: Merge PDF
            "tool_merge_title" to "Combinar PDF",
            "tool_merge_desc" to "Combine varios documentos en un solo PDF.",
            "tool_merge_add" to "Añadir Archivo PDF",
            "tool_merge_queue" to "Cola de Compilación de Documentos:",
            "tool_merge_button" to "Compilar y Combinar PDFs",
            "tool_merge_empty" to "La cola está vacía. Seleccione archivos para combinar.",

            // Tool 5: Sign PDF
            "tool_sign_title" to "Firmar PDF",
            "tool_sign_desc" to "Autentique archivos con firmas dibujadas a mano.",
            "tool_sign_pad" to "Firma Digital (Táctil/Lápiz)",
            "tool_sign_clear" to "Limpiar Panel",
            "tool_sign_burn" to "Firmar e Incrustar en el PDF",
            "tool_sign_tip" to "Dibuje su firma abajo y toque para incrustar en el documento.",

            // Tool 6: Encrypt PDF
            "tool_encrypt_title" to "Cifrar PDF",
            "tool_encrypt_desc" to "Proteja PDFs confidenciales con un casillero AES-256.",
            "tool_encrypt_password" to "Contraseña de Cifrado:",
            "tool_encrypt_button" to "Asegurar y Cifrar Archivo",
            "tool_encrypt_secured" to "¡Archivo guardado en el casillero seguro con éxito!",

            // Tool 7: Watermark PDF
            "tool_watermark_title" to "Marca de Agua",
            "tool_watermark_desc" to "Superponga texto de marca de agua o sellos.",
            "tool_watermark_text" to "Texto de la Marca de Agua:",
            "tool_watermark_placeholder" to "CONFIDENCIAL",
            "tool_watermark_opacity" to "Opacidad:",
            "tool_watermark_rotation" to "Ángulo de rotación:",
            "tool_watermark_button" to "Estampar y Superponer PDF",

            // Tool 8: PDF to Image
            "tool_pdftoimg_title" to "PDF a Imagen",
            "tool_pdftoimg_desc" to "Exporte páginas directamente a imágenes JPEG de alta calidad.",
            "tool_pdftoimg_button" to "Exportar Páginas PDF a Imágenes",

            // Tool 9: Extract Images
            "tool_extract_title" to "Extraer Imágenes",
            "tool_extract_desc" to "Extraiga y guarde las ilustraciones de los documentos.",
            "tool_extract_button" to "Extraer y Guardar Gráficos",

            // Permissions
            "perm_title" to "Acceso de Almacenamiento Requerido",
            "perm_rationale" to "OmniPDF procesa todos los documentos en su dispositivo para proteger su privacidad. Requiere permisos para leer/escribir PDFs locales.",
            "perm_grant" to "Conceder Permiso",
            "perm_manual" to "Abrir Ajustes",
            "perm_alert" to "Se requieren permisos para utilizar estas utilidades sin conexión."
        ),
        "fr" to mapOf(
            "app_name" to "OmniPDF",
            "greeting" to "Bienvenue sur OmniPDF",
            "subtitle" to "Utilitaires PDF dynamiques sur l'appareil",
            "settings" to "Paramètres",
            "appearance" to "Paramètres d'Apparence",
            "theme_label" to "Option de Thème",
            "theme_light" to "Classic Light (Gris Doux/Charbon)",
            "theme_dark" to "Pitch Dark (Noir OLED Absolu)",
            "typography_label" to "Style de Typographie",
            "typo_sans" to "Sans-Serif Moderne",
            "typo_mono" to "Monospacé Technique",
            "language_label" to "Langue de l'Application",
            "lang_en" to "Anglais (en)",
            "lang_es" to "Espagnol (es)",
            "lang_fr" to "Français (fr)",
            "save" to "Enregistrer",
            "back" to "Retour",
            "close" to "Fermer",
            "status_ready" to "Prêt",
            "status_processing" to "Traitement local en cours...",
            "status_success" to "Opération terminée avec succès !",
            "status_error" to "Une erreur est survenue :",

            // Tool 1: Scanner
            "tool_scanner_title" to "Scanner le Document",
            "tool_scanner_desc" to "Convertir des papiers physiques en PDF via l'appareil photo.",
            "tool_scanner_snap" to "Prendre Photo",
            "tool_scanner_simulate" to "Simuler la Photo (PDF haute fidélité)",
            "tool_scanner_compile" to "Compiler les Pages en PDF",
            "tool_scanner_scanned_pages" to "Pages Capturées :",
            "tool_scanner_empty" to "Aucune page capturée. Appuyez ci-dessous pour prendre une photo.",

            // Tool 2: PDF Viewer
            "tool_viewer_title" to "Lecteur PDF",
            "tool_viewer_desc" to "Explorateur de documents avec zoom par pincement.",
            "tool_viewer_select" to "Sélectionner un Fichier PDF",
            "tool_viewer_no_file" to "Aucun fichier PDF sélectionné.",
            "tool_viewer_page_index" to "Page :",
            "tool_viewer_zoom_tip" to "Pincez l'écran pour zoomer l'image du document.",

            // Tool 3: Split PDF
            "tool_split_title" to "Diviser le PDF",
            "tool_split_desc" to "Extraire des segments de pages ou des plages spécifiques.",
            "tool_split_range" to "Plage de Pages (ex. 1-2, 4) :",
            "tool_split_button" to "Diviser et Exporter les Pages",
            "tool_split_placeholder" to "ex., 1-2, 5",

            // Tool 4: Merge PDF
            "tool_merge_title" to "Fusionner des PDF",
            "tool_merge_desc" to "Combiner plusieurs fichiers locaux en un seul PDF.",
            "tool_merge_add" to "Ajouter un Fichier PDF",
            "tool_merge_queue" to "File de Compilation des Documents :",
            "tool_merge_button" to "Compiler et Fusionner les PDF",
            "tool_merge_empty" to "La file d'attente est vide. Sélectionnez des fichiers à fusionner.",

            // Tool 5: Sign PDF
            "tool_sign_title" to "Signer le PDF",
            "tool_sign_desc" to "Authentifier des fichiers avec des signatures manuscrites.",
            "tool_sign_pad" to "Zone de Signature Numérique (Tactile/Stylet)",
            "tool_sign_clear" to "Effacer",
            "tool_sign_burn" to "Signer et Appliquer au PDF",
            "tool_sign_tip" to "Dessinez votre signature et appuyez pour l'intégrer au PDF.",

            // Tool 6: Encrypt PDF
            "tool_encrypt_title" to "Chiffrer le PDF",
            "tool_encrypt_desc" to "Verrouiller des PDF confidentiels avec un coffre-fort AES-256.",
            "tool_encrypt_password" to "Mot de Passe de Chiffrement :",
            "tool_encrypt_button" to "Sécuriser et Chiffrer le Fichier",
            "tool_encrypt_secured" to "Fichier placé dans le coffre-fort sécurisé !",

            // Tool 7: Watermark PDF
            "tool_watermark_title" to "Filigrane PDF",
            "tool_watermark_desc" to "Superposer du texte personnalisé ou des tampons de brouillon.",
            "tool_watermark_text" to "Texte du Filigrane :",
            "tool_watermark_placeholder" to "CONFIDENTIEL",
            "tool_watermark_opacity" to "Opacité :",
            "tool_watermark_rotation" to "Angle de rotation :",
            "tool_watermark_button" to "Appliquer le Filigrane",

            // Tool 8: PDF to Image
            "tool_pdftoimg_title" to "PDF en Image",
            "tool_pdftoimg_desc" to "Exporter des pages directement en images JPEG haute qualité.",
            "tool_pdftoimg_button" to "Exporter les Pages PDF en Images",

            // Tool 9: Extract Images
            "tool_extract_title" to "Extraire des Images",
            "tool_extract_desc" to "Extraire et isoler les illustrations des documents.",
            "tool_extract_button" to "Extraire et Enregistrer les Images",

            // Permissions
            "perm_title" to "Accès Stockage Requis",
            "perm_rationale" to "OmniPDF traite tous vos documents sur votre appareil pour protéger votre vie privée. L'accès au stockage est requis pour lire/écrire vos fichiers.",
            "perm_grant" to "Accorder la Permission",
            "perm_manual" to "Ouvrir les Paramètres",
            "perm_alert" to "Les permissions sont requises pour exécuter ces utilitaires hors ligne."
        )
    )

    fun get(key: String, lang: String): String {
        val dict = translations[lang] ?: translations["en"]!!
        return dict[key] ?: translations["en"]?.get(key) ?: key
    }
}
