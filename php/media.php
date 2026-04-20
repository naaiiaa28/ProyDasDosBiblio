<?php
include 'conexion.php';

$accion = $_POST["accion"];
$usuario_id = $_POST["usuario_id"];

if ($accion == "obtener_todos") {
    $resultado = mysqli_query($con, 
        "SELECT * FROM media WHERE usuario_id='$usuario_id' ORDER BY fecha_adicion DESC");
    $lista = array();
    while ($fila = mysqli_fetch_assoc($resultado)) {
        $lista[] = $fila;
    }
    echo json_encode($lista);

} else if ($accion == "obtener_por_id") {
    $id = $_POST["id"];
    $resultado = mysqli_query($con, 
        "SELECT * FROM media WHERE id='$id' AND usuario_id='$usuario_id'");
    $fila = mysqli_fetch_assoc($resultado);
    echo json_encode($fila);

} else if ($accion == "filtrar_estado") {
    $estado = $_POST["estado"];
    $resultado = mysqli_query($con, 
        "SELECT * FROM media WHERE usuario_id='$usuario_id' AND estado='$estado' ORDER BY fecha_adicion DESC");
    $lista = array();
    while ($fila = mysqli_fetch_assoc($resultado)) {
        $lista[] = $fila;
    }
    echo json_encode($lista);

} else if ($accion == "insertar") {
    $titulo = $_POST["titulo"];
    $tipo = $_POST["tipo"];
    $genero = $_POST["genero"];
    $puntuacion = $_POST["puntuacion"];
    $comentario = $_POST["comentario"];
    $resumen = $_POST["resumen"];
    $estado = $_POST["estado"];
    $temporadas_totales = $_POST["temporadas_totales"];
    $temporada_actual = $_POST["temporada_actual"];
    $capitulo_actual = $_POST["capitulo_actual"];

    $sql = "INSERT INTO media (usuario_id, titulo, tipo, genero, puntuacion, comentario, resumen, estado, temporadas_totales, temporada_actual, capitulo_actual) 
            VALUES ('$usuario_id', '$titulo', '$tipo', '$genero', '$puntuacion', '$comentario', '$resumen', '$estado', '$temporadas_totales', '$temporada_actual', '$capitulo_actual')";
    
    if (mysqli_query($con, $sql)) {
        echo json_encode(array("resultado" => "ok", "id" => mysqli_insert_id($con)));
    } else {
        echo json_encode(array("resultado" => "error"));
    }

} else if ($accion == "actualizar") {
    $id = $_POST["id"];
    $titulo = $_POST["titulo"];
    $tipo = $_POST["tipo"];
    $genero = $_POST["genero"];
    $puntuacion = $_POST["puntuacion"];
    $comentario = $_POST["comentario"];
    $resumen = $_POST["resumen"];
    $estado = $_POST["estado"];
    $temporadas_totales = $_POST["temporadas_totales"];
    $temporada_actual = $_POST["temporada_actual"];
    $capitulo_actual = $_POST["capitulo_actual"];

    $sql = "UPDATE media SET titulo='$titulo', tipo='$tipo', genero='$genero', 
            puntuacion='$puntuacion', comentario='$comentario', resumen='$resumen', 
            estado='$estado', temporadas_totales='$temporadas_totales', 
            temporada_actual='$temporada_actual', capitulo_actual='$capitulo_actual'
            WHERE id='$id' AND usuario_id='$usuario_id'";

    if (mysqli_query($con, $sql)) {
        echo json_encode(array("resultado" => "ok"));
    } else {
        echo json_encode(array("resultado" => "error"));
    }

} else if ($accion == "eliminar") {
    $id = $_POST["id"];
    if (mysqli_query($con, "DELETE FROM media WHERE id='$id' AND usuario_id='$usuario_id'")) {
        echo json_encode(array("resultado" => "ok"));
    } else {
        echo json_encode(array("resultado" => "error"));
    }

} else if ($accion == "subir_imagen") {
    $id = $_POST["id"];
    
    if (isset($_FILES["imagen"]) && $_FILES["imagen"]["error"] == 0) {
        $dir = "/var/www/html/mipantalla/imagenes/";
        if (!file_exists($dir)) mkdir($dir, 0755, true);
        
        $nombre = $usuario_id . "_" . $id . "_" . time() . ".jpg";
        $ruta = $dir . $nombre;
        
        if (move_uploaded_file($_FILES["imagen"]["tmp_name"], $ruta)) {
            $rutaWeb = "imagenes/" . $nombre;
            mysqli_query($con, "UPDATE media SET ruta_imagen='$rutaWeb' WHERE id='$id'");
            echo json_encode(array("resultado" => "ok", "ruta_imagen" => $rutaWeb));
        } else {
            echo json_encode(array("resultado" => "error_subida"));
        }
    } else {
        echo json_encode(array("resultado" => "no_imagen"));
    }
}

mysqli_close($con);
?>
