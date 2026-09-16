package com.academia.observer;

/**
 * Interface Observer — Padrão de Projeto Comportamental Observer.
 *
 * <p>Define o contrato para todos os objetos que desejam ser notificados
 * sobre mudanças de estado em um {@link Subject}.</p>
 *
 * <p>No contexto deste sistema:
 * <ul>
 *   <li>O {@code PainelPrincipalController} implementa esta interface.</li>
 *   <li>Ele é notificado sempre que um novo pagamento é registrado.</li>
 * </ul>
 * </p>
 */
public interface Observer {

    /**
     * Método chamado pelo {@link Subject} quando ocorre uma atualização.
     *
     * @param evento Descrição do evento que ocorreu (ex.: "PAGAMENTO_REGISTRADO").
     * @param dado   Objeto com informação relevante (ex.: valor do pagamento como Double).
     */
    void atualizar(String evento, Object dado);
}

