/**
 * Renders pixel grid is container.
 */
export function renderGrid(container: HTMLElement): void {
    const canvas = document.createElement('canvas');
    canvas.width = container.clientWidth;
    canvas.height = container.clientHeight;
    canvas.style.position = 'absolute';
    canvas.style.top = '0px';
    canvas.style.left = '0px';
    canvas.style.width = '100%';
    canvas.style.height = '100%';

    container.appendChild(canvas);

    const context = canvas.getContext('2d')!;

    const TEXT_SIZE = 14;
    context.fillStyle = '#FF0000FF';
    context.font = TEXT_SIZE + 'px Arial';

    context.strokeStyle = '#FF00FF44';
    for (let x = 0; x < container.clientWidth; x += 10) {
        context.beginPath();
        context.moveTo(x, 0);
        context.lineTo(x, container.clientHeight);
        context.stroke();
    }

    context.strokeStyle = '#FF00FFFF';
    for (let x = 0; x < container.clientWidth; x += 100) {
        context.beginPath();
        context.moveTo(x, 0);
        context.lineTo(x, container.clientHeight);
        context.stroke();
    }

    for (let x = 0; x < container.clientWidth; x += 100) {
        context.beginPath();
        context.fillText(String(x), x + 1, TEXT_SIZE);
        context.stroke();
    }

    context.strokeStyle = '#FF00FF55';
    for (let x = 50; x < container.clientWidth; x += 100) {
        context.beginPath();
        context.moveTo(x, 0);
        context.lineTo(x, container.clientHeight);
        context.stroke();
    }

    context.strokeStyle = '#FF00FF44';
    for (let y = 0; y < container.clientHeight; y += 10) {
        context.beginPath();
        context.moveTo(0, y);
        context.lineTo(container.clientWidth, y);
        context.stroke();
    }

    context.strokeStyle = '#FF00FFFF';
    for (let y = 0; y < container.clientHeight; y += 100) {
        context.beginPath();
        context.moveTo(0, y);
        context.lineTo(container.clientWidth, y);
        context.stroke();
    }

    for (let y = 0; y < container.clientHeight; y += 100) {
        context.beginPath();
        context.fillText(String(y), 1, y + TEXT_SIZE);
        context.stroke();
    }

    context.strokeStyle = '#FF00FF55';
    for (let y = 50; y < container.clientHeight; y += 100) {
        context.beginPath();
        context.moveTo(0, y);
        context.lineTo(container.clientWidth, y);
        context.stroke();
    }
}
