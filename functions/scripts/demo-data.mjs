export const demoVersion = "split-the-bills-demo-v1";
export const demoProjectId = "demo-split-the-bills";
export const demoPassword = "SplitDemo123!";
export const demoUsers = [
  { key: "alice", uid: "demo-alice", email: "alice@demo.test", displayName: "Alice Morgan" },
  { key: "bob", uid: "demo-bob", email: "bob@demo.test", displayName: "Bob Chen" },
  { key: "cara", uid: "demo-cara", email: "cara@demo.test", displayName: "Cara Silva" },
  { key: "dana", uid: "demo-dana", email: "dana@demo.test", displayName: "Dana Patel" },
];

const equal = (...people) => ({ mode: "EQUAL", entries: people.map((person) => ({ person })) });
const exact = (values) => ({ mode: "EXACT", entries: Object.entries(values).map(([person, amountMinor]) => ({ person, amountMinor })) });
const percent = (values) => ({ mode: "PERCENTAGE", entries: Object.entries(values).map(([person, basisPoints]) => ({ person, basisPoints })) });
const weighted = (values) => ({ mode: "WEIGHTED", entries: Object.entries(values).map(([person, shares]) => ({ person, shares })) });
const trio = ["alice", "bob", "cara"];

export const demoGroups = [
  {
    key: "lisbon", id: "demo-lisbon-weekend", name: "Lisbon weekend", type: "TRIP", currency: "EUR",
    owner: "alice", members: trio, guests: ["dana"],
    expenses: [
      { key: "apartment", title: "Coastal apartment", amountMinor: 24000, payer: "alice", split: equal(...trio), category: "LODGING", daysAgo: 10, note: "Three nights by the coast" },
      { key: "dinner", title: "Dinner by the river", amountMinor: 9000, payer: "bob", split: equal(...trio), category: "FOOD", daysAgo: 8, note: "A shared evening in Alfama" },
      { key: "transfers", title: "Airport transfers", amountMinor: 4500, payer: "cara", split: weighted({ alice: 2, bob: 1, cara: 1 }), category: "TRANSPORT", daysAgo: 7, note: "Alice took an extra airport trip" },
      { key: "museum", title: "Museum tickets", amountMinor: 3000, payer: "alice", split: exact({ alice: 1000, bob: 1200, cara: 800 }), category: "ENTERTAINMENT", daysAgo: 5, note: "Different ticket prices" },
      { key: "coffee", title: "Coffee & pastries", amountMinor: 1200, payer: "bob", split: percent({ alice: 5000, bob: 2500, cara: 2500 }), category: "FOOD", daysAgo: 0, note: "Alice also picked up breakfast for later" },
    ],
    settlements: [{ key: "bob-partial", from: "bob", to: "alice", amountMinor: 2000, daysAgo: 1, note: "Partial repayment after the trip" }],
  },
  {
    key: "home", id: "demo-shared-apartment", name: "Shared apartment", type: "HOME", currency: "EUR",
    owner: "bob", members: trio, guests: [],
    expenses: [
      { key: "groceries", title: "Monthly groceries", amountMinor: 9600, payer: "bob", split: equal(...trio), category: "GROCERIES", daysAgo: 6, note: "The shared kitchen shop" },
      { key: "internet", title: "Internet bill", amountMinor: 4500, payer: "bob", split: exact({ alice: 1500, bob: 1500, cara: 1500 }), category: "OTHER", daysAgo: 3, note: "One month of home internet" },
      { key: "cleaning", title: "Cleaning supplies", amountMinor: 1800, payer: "cara", split: equal(...trio), category: "GROCERIES", daysAgo: 0, note: "For the apartment" },
    ],
    settlements: [{ key: "alice-partial", from: "alice", to: "bob", amountMinor: 1500, daysAgo: 0, note: "Part of this month's home expenses" }],
  },
  {
    key: "movies", id: "demo-movie-night", name: "Friday movie night", type: "FRIENDS", currency: "USD",
    owner: "cara", members: trio, guests: [],
    expenses: [
      { key: "tickets", title: "Cinema tickets", amountMinor: 6000, payer: "cara", split: equal(...trio), category: "ENTERTAINMENT", daysAgo: 4, note: "Three seats for the evening show" },
      { key: "snacks", title: "Snacks & drinks", amountMinor: 2400, payer: "alice", split: equal(...trio), category: "FOOD", daysAgo: 0, note: "Popcorn to share" },
    ],
    settlements: [{ key: "bob-partial", from: "bob", to: "cara", amountMinor: 1000, daysAgo: 0, note: "Paid outside the app" }],
  },
  {
    key: "mountains", id: "demo-mountain-trip", name: "Mountain day trip", type: "TRIP", currency: "EUR",
    owner: "alice", members: ["alice", "bob"], guests: [], archived: true,
    expenses: [{ key: "picnic", title: "Trail picnic", amountMinor: 3000, payer: "alice", split: equal("alice", "bob"), category: "FOOD", daysAgo: 12, note: "A completed shared plan" }],
    settlements: [{ key: "bob-full", from: "bob", to: "alice", amountMinor: 1500, daysAgo: 11, note: "All settled" }],
  },
  {
    key: "weekend", id: "demo-weekend-plans", name: "Weekend plans", type: "COUPLE", currency: "GBP",
    owner: "bob", members: ["bob", "cara"], guests: [], expenses: [], settlements: [],
  },
];

export function participantId(group, person) { return `demo-${group.key}-${person}`; }

export function demoDate(referenceDate, daysAgo) {
  const date = new Date(`${referenceDate}T12:00:00.000Z`);
  date.setUTCDate(date.getUTCDate() - daysAgo);
  return date.toISOString().slice(0, 10);
}

export function expensePayload(group, expense, referenceDate) {
  return {
    title: expense.title, amountMinor: expense.amountMinor, currency: group.currency,
    contributions: [{ participantId: participantId(group, expense.payer), amountMinor: expense.amountMinor }],
    split: { mode: expense.split.mode, entries: expense.split.entries.map(({ person, ...value }) => ({ participantId: participantId(group, person), ...value })) },
    date: demoDate(referenceDate, expense.daysAgo), category: expense.category, note: expense.note,
  };
}

export function settlementPayload(group, settlement, referenceDate) {
  return {
    fromParticipantId: participantId(group, settlement.from), toParticipantId: participantId(group, settlement.to),
    amountMinor: settlement.amountMinor, currency: group.currency,
    date: demoDate(referenceDate, settlement.daysAgo), note: settlement.note,
  };
}
