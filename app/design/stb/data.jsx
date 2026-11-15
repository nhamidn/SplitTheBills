// Split the Bills — seed data + balance engine

const STB_PEOPLE = {
  me:    { id: 'me',    name: 'Maya Chen',     short: 'You',   hue: 160 },
  sam:   { id: 'sam',   name: 'Sam Rivera',    short: 'Sam',   hue: 250 },
  priya: { id: 'priya', name: 'Priya Shah',    short: 'Priya', hue: 25 },
  leo:   { id: 'leo',   name: 'Leo Martins',   short: 'Leo',   hue: 75 },
  jonas: { id: 'jonas', name: 'Jonas Becker',  short: 'Jonas', hue: 210 },
  aiko:  { id: 'aiko',  name: 'Aiko Tanaka',   short: 'Aiko',  hue: 330 },
  noah:  { id: 'noah',  name: 'Noah Kim',      short: 'Noah',  hue: 120 },
};

const STB_GROUPS_SEED = [
  { id: 'lisbon', name: 'Lisbon Trip',    kind: 'Trip',    icon: 'flight',     hue: 200, dates: 'Nov 8 – 12', members: ['me', 'priya', 'leo', 'jonas', 'aiko'] },
  { id: 'elm',    name: '42 Elm Street',  kind: 'Home',    icon: 'home',       hue: 150, dates: 'Roommates',  members: ['me', 'sam', 'noah'] },
  { id: 'friday', name: 'Friday Dinners', kind: 'Friends', icon: 'restaurant', hue: 40,  dates: 'Weekly',     members: ['me', 'priya', 'sam', 'leo', 'aiko', 'noah'] },
  { id: 'couple', name: 'Maya & Sam',     kind: 'Couple',  icon: 'favorite',   hue: 15,  dates: 'Shared',     members: ['me', 'sam'] },
];

const STB_CATS = {
  food:      { label: 'Food',       icon: 'restaurant' },
  groceries: { label: 'Groceries',  icon: 'local_grocery_store' },
  stay:      { label: 'Stay',       icon: 'hotel' },
  transport: { label: 'Transport',  icon: 'local_taxi' },
  coffee:    { label: 'Coffee',     icon: 'local_cafe' },
  nightlife: { label: 'Nightlife',  icon: 'nightlife' },
  rent:      { label: 'Rent',       icon: 'key' },
  utilities: { label: 'Utilities',  icon: 'bolt' },
  internet:  { label: 'Internet',   icon: 'wifi' },
  home:      { label: 'Household',  icon: 'cleaning_services' },
  fun:       { label: 'Fun',        icon: 'confirmation_number' },
  other:     { label: 'Other',      icon: 'receipt_long' },
};

// date: ISO day (today = 2026-11-15)
function stbEqual(amount, ids) {
  const cents = Math.round(amount * 100);
  const base = Math.floor(cents / ids.length);
  let rem = cents - base * ids.length;
  const out = {};
  ids.forEach((id) => { out[id] = (base + (rem-- > 0 ? 1 : 0)) / 100; });
  return out;
}

const L = ['me', 'priya', 'leo', 'jonas', 'aiko'];
const E = ['me', 'sam', 'noah'];
const STB_EXPENSES_SEED = [
  { id: 'e1',  group: 'lisbon', title: 'Airbnb in Alfama',      cat: 'stay',      amount: 1240.00, paidBy: 'me',    date: '2026-11-08', split: 'equal', shares: stbEqual(1240, L) },
  { id: 'e2',  group: 'lisbon', title: 'Dinner at Taberna',     cat: 'food',      amount: 186.50,  paidBy: 'priya', date: '2026-11-09', split: 'equal', shares: stbEqual(186.5, L) },
  { id: 'e3',  group: 'lisbon', title: 'Tuk-tuk city tour',     cat: 'transport', amount: 95.00,   paidBy: 'leo',   date: '2026-11-10', split: 'equal', shares: stbEqual(95, L) },
  { id: 'e4',  group: 'lisbon', title: 'Pastéis & coffee',      cat: 'coffee',    amount: 32.40,   paidBy: 'aiko',  date: '2026-11-10', split: 'equal', shares: stbEqual(32.4, L) },
  { id: 'e5',  group: 'lisbon', title: 'Sintra train tickets',  cat: 'transport', amount: 48.00,   paidBy: 'jonas', date: '2026-11-11', split: 'equal', shares: stbEqual(48, ['me', 'priya', 'leo', 'jonas']) },
  { id: 'e6',  group: 'lisbon', title: 'Fado night',            cat: 'nightlife', amount: 210.00,  paidBy: 'priya', date: '2026-11-11', split: 'equal', shares: stbEqual(210, L) },
  { id: 'e7',  group: 'elm',    title: 'November rent',         cat: 'rent',      amount: 2850.00, paidBy: 'sam',   date: '2026-11-01', split: 'custom', shares: { me: 1050, sam: 1050, noah: 750 } },
  { id: 'e9',  group: 'elm',    title: 'Fiber internet',        cat: 'internet',  amount: 69.99,   paidBy: 'noah',  date: '2026-11-05', split: 'equal', shares: stbEqual(69.99, E) },
  { id: 'e8',  group: 'elm',    title: 'Electricity bill',      cat: 'utilities', amount: 142.36,  paidBy: 'me',    date: '2026-11-12', split: 'equal', shares: stbEqual(142.36, E) },
  { id: 'e11', group: 'elm',    title: 'Cleaning supplies',     cat: 'home',      amount: 36.80,   paidBy: 'sam',   date: '2026-11-13', split: 'equal', shares: stbEqual(36.8, E) },
  { id: 'e10', group: 'elm',    title: "Weekly groceries",      cat: 'groceries', amount: 118.42,  paidBy: 'me',    date: '2026-11-14', split: 'equal', shares: stbEqual(118.42, E) },
  { id: 'e12', group: 'friday', title: 'Ramen Kazu',            cat: 'food',      amount: 164.00,  paidBy: 'leo',   date: '2026-11-07', split: 'equal', shares: stbEqual(164, ['me', 'priya', 'sam', 'leo', 'aiko', 'noah']) },
  { id: 'e13', group: 'friday', title: 'Wine bar on 5th',       cat: 'nightlife', amount: 132.00,  paidBy: 'me',    date: '2026-11-14', split: 'percent', shares: { me: 39.6, priya: 39.6, aiko: 26.4, noah: 26.4 } },
  { id: 'e14', group: 'couple', title: 'Concert tickets',       cat: 'fun',       amount: 178.00,  paidBy: 'sam',   date: '2026-11-13', split: 'equal', shares: stbEqual(178, ['me', 'sam']) },
  { id: 'e15', group: 'couple', title: 'Sunday brunch',         cat: 'food',      amount: 64.20,   paidBy: 'me',    date: '2026-11-15', split: 'equal', shares: stbEqual(64.2, ['me', 'sam']) },
  { id: 'e16', group: 'couple', title: 'Ride home',             cat: 'transport', amount: 23.80,   paidBy: 'me',    date: '2026-11-15', split: 'equal', shares: stbEqual(23.8, ['me', 'sam']) },
];

const STB_SETTLEMENTS_SEED = [
  { id: 's1', group: 'elm',    from: 'me',    to: 'sam', amount: 1050.00, date: '2026-11-02', method: 'Bank transfer', status: 'completed' },
  { id: 's2', group: 'elm',    from: 'noah',  to: 'sam', amount: 750.00,  date: '2026-11-02', method: 'Bank transfer', status: 'completed' },
  { id: 's3', group: 'lisbon', from: 'jonas', to: 'me',  amount: 200.00,  date: '2026-11-13', method: 'Bank transfer', status: 'completed' },
  { id: 's4', group: 'friday', from: 'me',    to: 'leo', amount: 27.33,   date: '2026-11-08', method: 'Cash',          status: 'completed' },
  { id: 's5', group: 'lisbon', from: 'aiko',  to: 'me',  amount: 150.00,  date: '2026-11-14', method: 'Bank transfer', status: 'pending' },
];

// ── engine ────────────────────────────────────────────────
const r2 = (n) => Math.round(n * 100) / 100;

function stbNet(groupId, expenses, settlements) {
  const net = {};
  const add = (id, v) => { net[id] = r2((net[id] || 0) + v); };
  expenses.filter((e) => !groupId || e.group === groupId).forEach((e) => {
    add(e.paidBy, e.amount);
    Object.entries(e.shares).forEach(([id, s]) => add(id, -s));
  });
  settlements.filter((s) => s.status === 'completed' && (!groupId || s.group === groupId)).forEach((s) => {
    add(s.from, s.amount);
    add(s.to, -s.amount);
  });
  return net;
}

// greedy min-transfer simplification
function stbSimplify(net) {
  const cred = [], debt = [];
  Object.entries(net).forEach(([id, v]) => {
    if (v > 0.009) cred.push({ id, v });
    else if (v < -0.009) debt.push({ id, v: -v });
  });
  const out = [];
  while (cred.length && debt.length) {
    cred.sort((a, b) => b.v - a.v); debt.sort((a, b) => b.v - a.v);
    const c = cred[0], d = debt[0];
    const amt = r2(Math.min(c.v, d.v));
    out.push({ from: d.id, to: c.id, amount: amt });
    c.v = r2(c.v - amt); d.v = r2(d.v - amt);
    if (c.v < 0.01) cred.shift();
    if (d.v < 0.01) debt.shift();
  }
  return out;
}

// count naive pairwise debts (before simplification)
function stbRawPairs(groupId, expenses) {
  const pairs = new Set();
  expenses.filter((e) => e.group === groupId).forEach((e) => {
    Object.keys(e.shares).forEach((id) => { if (id !== e.paidBy) pairs.add([id, e.paidBy].sort().join('|')); });
  });
  return pairs.size;
}

function stbMyShare(e) {
  const mine = e.shares.me || 0;
  if (e.paidBy === 'me') return { kind: 'lent', value: r2(e.amount - mine) };
  if (mine > 0) return { kind: 'owe', value: mine };
  return { kind: 'none', value: 0 };
}

const stbFmt = (n, sign = false) => {
  const s = '$' + Math.abs(n).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  if (!sign) return s;
  return (n > 0.004 ? '+' : n < -0.004 ? '−' : '') + s;
};

const STB_TODAY = '2026-11-15';
function stbDayLabel(iso) {
  if (iso === STB_TODAY) return 'Today';
  if (iso === '2026-11-14') return 'Yesterday';
  const d = new Date(iso + 'T12:00:00');
  return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
}
function stbShortDate(iso) {
  const d = new Date(iso + 'T12:00:00');
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

Object.assign(window, {
  STB_PEOPLE, STB_GROUPS_SEED, STB_CATS, STB_EXPENSES_SEED, STB_SETTLEMENTS_SEED, STB_TODAY,
  stbEqual, stbNet, stbSimplify, stbRawPairs, stbMyShare, stbFmt, stbDayLabel, stbShortDate, r2,
});
