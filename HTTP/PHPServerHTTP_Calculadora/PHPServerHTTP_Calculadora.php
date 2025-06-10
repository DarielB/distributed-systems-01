<?php
// Calculadora remota via POST

// Função para validar e converter valores
function getPostValue($key) {
    return isset($_POST[$key]) ? floatval($_POST[$key]) : null;
}

// Recebe o valor da primeira operação
$oper1 = getPostValue('oper1');
// Recebe o valor da segunda operação
$oper2 = getPostValue('oper2');

// Recebe o valor do tipo de operação
$operacao = isset($_POST['operacao']) ? intval($_POST['operacao']) : null;

$resultado = null;
$erro = null;

// Verifica se uma das operações ou o tipo de operação é null e exibe mensagem de parÂmetros inválidos
if ($oper1 === null || $oper2 === null || $operacao === null) {
    $erro = "Parâmetros inválidos. Envie 'oper1', 'oper2' e 'operacao' via POST.";
} else {

    // Efetua o cálculo de acordo com o tipo de operação requisitada
    switch ($operacao) {
        case 1: // Soma
            $resultado = $oper1 + $oper2;
            break;
        case 2: // Subtração
            $resultado = $oper1 - $oper2;
            break;
        case 3: // Multiplicação
            $resultado = $oper1 * $oper2;
            break;
        case 4: // Divisão
            if ($oper2 == 0) {
                $erro = "Erro: divisão por zero.";
            } else {
                $resultado = $oper1 / $oper2;
            }
            break;
        default:
            $erro = "Operação inválida. Use 1 (soma), 2 (subtração), 3 (multiplicação) ou 4 (divisão).";
    }
}

// Retorno em JSON
header('Content-Type: application/json');

if ($erro) {
    echo json_encode(["erro" => $erro]);
} else {
    echo json_encode([
        "oper1" => $oper1,
        "oper2" => $oper2,
        "operacao" => $operacao,
        "resultado" => $resultado
    ]);
}