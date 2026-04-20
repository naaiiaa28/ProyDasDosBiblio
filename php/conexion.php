<?php
$DB_SERVER = "localhost";
$DB_USER = "naia";
$DB_PASS = "1234";
$DB_DATABASE = "mipantalla";

$con = mysqli_connect($DB_SERVER, $DB_USER, $DB_PASS, $DB_DATABASE);

if (mysqli_connect_errno()) {
    echo json_encode(array("error" => "Error de conexión: " . mysqli_connect_error()));
    exit();
}

mysqli_set_charset($con, "utf8");
?>
