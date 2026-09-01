export function notificationsSupported() {
  return typeof window !== "undefined" && "Notification" in window;
}

export async function requestNotificationPermission() {
  if (!notificationsSupported()) return false;
  if (Notification.permission === "granted") return true;
  if (Notification.permission === "denied") return false;
  try {
    const res = await Notification.requestPermission();
    return res === "granted";
  } catch {
    return false;
  }
}

export function fireReminder(title, body) {
  if (notificationsSupported() && Notification.permission === "granted") {
    try {
      new Notification(title, { body, tag: title });
    } catch {
      /* ignore */
    }
  }
}