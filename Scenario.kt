package com.autolauncher

/** Шаги сценария. Координаты тапов — доли экрана (0.0..1.0), т.к. игры на Unity не отдают элементы UI. */
sealed class Step {
    data class Wait(val ms: Long) : Step()
    data class Tap(val x: Float, val y: Float, val note: String = "") : Step()
    data class Chat(val text: String) : Step()   // тап по полю чата + ввод + отправка
    data class Type(val text: String) : Step()   // ввод в поле, которое уже в фокусе
}

class Scenario(val id: String, val title: String, val packages: List<String>, val steps: List<Step>)

object Scenarios {
    const val TRANSFER_ACCOUNT = "366263"
    const val PROMO = "alenka"

    // Координаты ниже — заготовки. Откалибруй под свой экран (см. README).
    private val CHAT_BTN = 0.05f to 0.10f
    private val CHAT_SEND = 0.95f to 0.90f

    val blackRussia = Scenario(
        id = "black_russia",
        title = "Black Russia",
        packages = listOf("com.br.top", "com.blackrussia.game"), // проверь имя пакета на телефоне
        steps = listOf(
            Step.Wait(30_000),                                   // загрузка игры
            // 1. Новый аккаунт
            Step.Tap(0.50f, 0.80f, "Новый аккаунт / Регистрация"),
            Step.Wait(2000),
            Step.Tap(0.50f, 0.45f, "Поле ника"),
            Step.Type("Alex_Smirnov"),
            Step.Tap(0.50f, 0.85f, "Продолжить"),
            Step.Wait(5000),
            // 2. Обучение (дядя Слава) — многократно жмём «далее/пропустить»
            *Array(25) { listOf(Step.Tap(0.50f, 0.88f, "Далее"), Step.Wait(2500)) }.flatMap { it }.toTypedArray(),
            // 3. Промокод
            Step.Tap(0.50f, 0.50f, "Кнопка /promo (если есть в меню)"),
            Step.Chat("/promo"),
            Step.Wait(1500),
            Step.Type(PROMO),
            Step.Tap(0.50f, 0.60f, "Подтвердить промокод"),
            Step.Wait(2000),
            // 4. Награды
            Step.Chat("/reward"),
            Step.Wait(2000),
            Step.Tap(0.30f, 0.50f, "Забрать приз 1"),
            Step.Tap(0.50f, 0.50f, "Забрать приз 2"),
            Step.Tap(0.70f, 0.50f, "Забрать приз 3 (купон x2 НЕ брать)"),
            Step.Tap(0.95f, 0.08f, "Закрыть"),
            // 5. Квесты
            Step.Chat("/quest"),
            Step.Wait(2000),
            Step.Tap(0.50f, 0.70f, "Покупка скина"),
            Step.Wait(1500),
            Step.Tap(0.50f, 0.80f, "Купить / надеть"),
            Step.Tap(0.50f, 0.50f, "Задание «Банковские услуги»"),
            Step.Tap(0.50f, 0.85f, "Взять задание"),
            // 6. Банкомат
            Step.Chat("/gps"),
            Step.Wait(1500),
            Step.Tap(0.50f, 0.40f, "Пункт «Банкомат»"),
            Step.Wait(40_000),                                   // дойти до банкомата
            Step.Tap(0.50f, 0.50f, "Открыть банкомат"),
            Step.Tap(0.50f, 0.60f, "Положить всё на счёт"),
            Step.Tap(0.50f, 0.70f, "Перевод"),
            Step.Tap(0.50f, 0.40f, "Поле номера счёта"),
            Step.Type(TRANSFER_ACCOUNT),
            Step.Tap(0.50f, 0.85f, "Подтвердить перевод")
        )
    )

    val all = listOf(blackRussia)
    fun byId(id: String) = all.firstOrNull { it.id == id }
    fun forPackage(pkg: String) = all.firstOrNull { pkg in it.packages }

    fun chatButton() = CHAT_BTN
    fun chatSend() = CHAT_SEND
}
