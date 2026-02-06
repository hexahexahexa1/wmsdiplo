#!/bin/bash
# Скрипт для быстрого тестирования импорта приходов

set -e

EXAMPLES_DIR="examples"
INCOMING_DIR="incoming"

echo "=== Тестирование импорта XML документов ==="
echo ""

# Проверка директорий
if [ ! -d "$EXAMPLES_DIR" ]; then
    echo "❌ Директория $EXAMPLES_DIR не найдена!"
    exit 1
fi

if [ ! -d "$INCOMING_DIR" ]; then
    echo "📁 Создание директории $INCOMING_DIR..."
    mkdir -p "$INCOMING_DIR"
fi

# Функция для импорта файла
import_file() {
    local file=$1
    local filename=$(basename "$file")
    
    echo "📦 Импорт: $filename"
    cp "$file" "$INCOMING_DIR/"
    
    echo "   ✅ Файл скопирован в $INCOMING_DIR/"
    echo "   ⏳ Ожидайте обработки import-service (до 10 секунд)..."
    echo ""
}

# Меню выбора
echo "Выберите тестовый файл для импорта:"
echo ""
echo "1) receipt-simple.xml     - Простой пример (3 позиции)"
echo "2) receipt-large.xml      - Большой заказ (12 позиций)"
echo "3) receipt-minimal.xml    - Минимальный пример (2 позиции)"
echo "4) receipt-weighted.xml   - Весовые товары (5 позиций)"
echo "5) receipt-pharma.xml     - Фармацевтика (5 позиций)"
echo "6) Все файлы сразу"
echo "0) Выход"
echo ""
read -p "Ваш выбор: " choice

case $choice in
    1)
        import_file "$EXAMPLES_DIR/receipt-simple.xml"
        ;;
    2)
        import_file "$EXAMPLES_DIR/receipt-large.xml"
        ;;
    3)
        import_file "$EXAMPLES_DIR/receipt-minimal.xml"
        ;;
    4)
        import_file "$EXAMPLES_DIR/receipt-weighted.xml"
        ;;
    5)
        import_file "$EXAMPLES_DIR/receipt-pharma.xml"
        ;;
    6)
        echo "📦 Импорт всех файлов..."
        echo ""
        for file in $EXAMPLES_DIR/*.xml; do
            import_file "$file"
        done
        ;;
    0)
        echo "Выход."
        exit 0
        ;;
    *)
        echo "❌ Неверный выбор!"
        exit 1
        ;;
esac

echo "=== Готово! ==="
echo ""
echo "Проверьте результат:"
echo "  • Desktop Client → Приходы"
echo "  • Логи import-service"
echo "  • Директория processed/ (успешные)"
echo "  • Директория failed/ (ошибки)"
