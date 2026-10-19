export function beijing(value: string): string {
 return new Date(value).toLocaleString('sv-SE', { timeZone: 'Asia/Shanghai', hour12: false }).replace(' ', 'T')
}
export function utc(value: string): string { return new Date(value + (value.length === 16 ? ':00' : '') + '+08:00').toISOString() }
export function futureDeadline(): string { return beijing(new Date(Date.now() + 2 * 86400000).toISOString()).slice(0, 10) + 'T21:00' }
