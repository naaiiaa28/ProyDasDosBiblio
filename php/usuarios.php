<?php
include 'conexion.php';

$accion = $_POST["accion"];

if ($accion == "registro") {

    $email = $_POST["email"];
    $password = password_hash($_POST["password"], PASSWORD_DEFAULT);
    $nombre = $_POST["nombre"];

    // Comprobar si el email ya existe
    $check = mysqli_query($con, "SELECT id FROM usuarios WHERE email='$email'");
    if (mysqli_num_rows($check) > 0) {
        echo json_encode(array("resultado" => "email_existente"));
        exit();
    }

    $sql = "INSERT INTO usuarios (email, password, nombre)
            VALUES ('$email', '$password', '$nombre')";

    if (mysqli_query($con, $sql)) {
        $id = mysqli_insert_id($con);
        echo json_encode(array("resultado" => "ok", "id" => $id, "nombre" => $nombre));
    } else {
        echo json_encode(array("resultado" => "error"));
    }

} else if ($accion == "login") {

    $email = $_POST["email"];
    $passwordIntento = $_POST["password"];

    $resultado = mysqli_query($con, "SELECT * FROM usuarios WHERE email='$email'");
    if (mysqli_num_rows($resultado) == 0) {
        echo json_encode(array("resultado" => "usuario_no_encontrado"));
        exit();
    }

    $fila = mysqli_fetch_assoc($resultado);

    if (password_verify($passwordIntento, $fila["password"])) {
        echo json_encode(array(
            "resultado" => "ok",
            "id" => $fila["id"],
            "nombre" => $fila["nombre"],
            "email" => $fila["email"],
            "ruta_foto" => $fila["ruta_foto"]
        ));
    } else {
        echo json_encode(array("resultado" => "password_incorrecto"));
    }

} else if ($accion == "obtener_perfil") {

    $id = $_POST["usuario_id"];

    $resultado = mysqli_query(
        $con,
        "SELECT id, nombre, email, ruta_foto FROM usuarios WHERE id='$id'"
    );

    $fila = mysqli_fetch_assoc($resultado);
    echo json_encode($fila);

} else if ($accion == "subir_foto") {

    $usuario_id = $_POST["usuario_id"];

    if (isset($_FILES["foto"]) && $_FILES["foto"]["error"] == 0) {

        $dir = "/var/www/html/mipantalla/fotos/";
        if (!file_exists($dir)) mkdir($dir, 0755, true);

        $resultado = mysqli_query($con,
            "SELECT ruta_foto FROM usuarios WHERE id='$usuario_id'");
        $fila = mysqli_fetch_assoc($resultado);

        if ($fila && !empty($fila["ruta_foto"])) {

            $rutaAnterior = "/var/www/html/mipantalla/" . $fila["ruta_foto"];

            if (file_exists($rutaAnterior)) {
                unlink($rutaAnterior);
            }
        }

        $nombre = $usuario_id . "_" . time() . ".jpg";
        $ruta = $dir . $nombre;

        if (move_uploaded_file($_FILES["foto"]["tmp_name"], $ruta)) {

            $rutaWeb = "fotos/" . $nombre;

            mysqli_query($con,
                "UPDATE usuarios SET ruta_foto='$rutaWeb' WHERE id='$usuario_id'");

            echo json_encode(array(
                "resultado" => "ok",
                "ruta_foto" => $rutaWeb
            ));

        } else {
            echo json_encode(array("resultado" => "error_subida"));
        }

    } else {
        echo json_encode(array("resultado" => "no_foto"));
    }
}

mysqli_close($con);
?>
