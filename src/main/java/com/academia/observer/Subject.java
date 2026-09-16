package com.academia.observer;

/**
 * Interface Subject — Padrão de Projeto Comportamental Observer.
 *
 * <p>Define o contrato para objetos que mantêm uma lista de {@link Observer}s
 * e os notificam quando seu estado muda.</p>
 *
 * <p>No contexto deste sistema:
 * <ul>
 *   <li>O {@code PagamentoController} implementa esta interface.</li>
 *   <li>Ao registrar um novo pagamento, notifica todos os observers cadastrados.</li>
 * </ul>
 * </p>
 */
public interface Subject {

    /**
     * Adiciona um observer à lista de observadores.
     *
     * @param observer O observer a ser registrado.
     */
    void adicionarObserver(Observer observer);

    /**
     * Remove um observer da lista de observadores.
     *
     * @param observer O observer a ser removido.
     */
    void removerObserver(Observer observer);

    /**
     * Notifica todos os observers registrados sobre uma mudança de estado.
     *
     * @param evento Descrição do evento (ex.: "PAGAMENTO_REGISTRADO").
     * @param dado   Objeto com a informação da atualização.
     */
    void notificarObservers(String evento, Object dado);
}

