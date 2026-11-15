// Balances & settlements

function DebtRow({ d, app, group, divider }) {
  const fromMe = d.from === 'me', toMe = d.to === 'me';
  const color = fromMe ? T.neg : toMe ? T.pos : T.ink;
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '14px 10px', margin: '0 4px', borderTop: divider ? `1px solid ${T.line}` : 0 }}>
      <Avatar id={d.from} size={36} />
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 6, minWidth: 0 }}>
        <div style={{ flex: 1, height: 2, background: `repeating-linear-gradient(90deg, ${color} 0 4px, transparent 4px 8px)`, opacity: 0.6, position: 'relative' }}>
          <div className="num" style={{ position: 'absolute', left: '50%', top: -24, transform: 'translateX(-50%)', fontSize: 14, fontWeight: 800, color, whiteSpace: 'nowrap' }}>{stbFmt(d.amount)}</div>
          <Icon name="chevron_right" size={18} color={color} style={{ position: 'absolute', right: -9, top: -8 }} />
        </div>
      </div>
      <Avatar id={d.to} size={36} />
      <div style={{ width: 92, textAlign: 'right' }}>
        {(fromMe || toMe) ? (
          <button className={fromMe ? 'btn-sm-dark' : 'btn-sm-tonal'} onClick={() => app.openSettle({ ...d, group })}>
            {fromMe ? 'Pay' : 'Record'}
          </button>
        ) : (
          <button className="btn-sm-tonal" onClick={() => app.openSettle({ ...d, group })}>Settle</button>
        )}
      </div>
    </div>
  );
}

function GroupBalances({ app, g, stats }) {
  const raw = stbRawPairs(g.id, app.expenses);
  const debts = stats.debts;
  const hist = app.settlements.filter((s) => s.group === g.id).sort((a, b) => b.date.localeCompare(a.date));
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px', borderRadius: 20, background: T.posSoft }}>
        <Icon name="auto_awesome" size={22} fill={1} color={T.pos} />
        <div style={{ flex: 1, fontSize: 13, fontWeight: 600, color: 'oklch(0.36 0.08 160)', lineHeight: 1.4 }}>
          Simplified from <b>{raw} debts</b> to <b>{debts.length} {debts.length === 1 ? 'transfer' : 'transfers'}</b>, so fewer payments change hands.
        </div>
      </div>
      {debts.length ? (
        <Card pad={6}>
          <div style={{ padding: '12px 14px 0', fontSize: 12, fontWeight: 700, color: T.ink3, letterSpacing: 0.4, textTransform: 'uppercase' }}>Who owes whom</div>
          {debts.map((d, i) => <DebtRow key={i} d={d} app={app} group={g.id} divider={i > 0} />)}
        </Card>
      ) : <AllSettled />}
      {hist.length > 0 && (
        <>
          <SectionTitle style={{ marginTop: 12, marginBottom: 4 }}>Payment history</SectionTitle>
          <Card pad={6}>{hist.map((s, i) => <SettlementRow key={s.id} s={s} divider={i > 0} app={app} />)}</Card>
        </>
      )}
    </div>
  );
}

function AllSettled() {
  return (
    <Card style={{ textAlign: 'center', padding: '28px 16px' }}>
      <div className="pop" style={{ width: 56, height: 56, borderRadius: 28, background: T.posSoft, color: T.pos, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
        <Icon name="done_all" size={28} />
      </div>
      <div style={{ fontSize: 16, fontWeight: 800, marginTop: 12 }}>All settled up</div>
      <div style={{ fontSize: 13, color: T.ink3, fontWeight: 500, marginTop: 4 }}>Nobody owes anything in this group.</div>
    </Card>
  );
}

function SettlementRow({ s, divider, app }) {
  const nm = (id) => id === 'me' ? 'You' : STB_PEOPLE[id].short;
  const g = app && app.groups.find((x) => x.id === s.group);
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 10px', margin: '0 4px', borderTop: divider ? `1px solid ${T.line}` : 0 }}>
      <div style={{ position: 'relative', width: 44, height: 44, flexShrink: 0 }}>
        <Avatar id={s.from} size={30} />
        <Avatar id={s.to} size={30} ring={T.surface} style={{ position: 'absolute', right: 0, bottom: 0 }} />
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14.5, fontWeight: 700 }}>{nm(s.from)} paid {nm(s.to)}</div>
        <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 500, marginTop: 2 }}>{stbShortDate(s.date)} · {s.method}{g ? ` · ${g.name}` : ''}</div>
      </div>
      <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
        <Money v={s.amount} size={15} />
        <StatusPill status={s.status} />
      </div>
    </div>
  );
}

// Overall balances screen (from Home "Settle up")
function BalancesScreen({ app }) {
  const stats = useGroupStats(app);
  const tot = useTotals(app, stats);
  // per-person net with me across groups (from simplified group debts)
  const people = {};
  Object.entries(stats).forEach(([gid, s]) => s.debts.forEach((d) => {
    if (d.from === 'me') { people[d.to] = people[d.to] || { v: 0, items: [] }; people[d.to].v -= d.amount; people[d.to].items.push({ ...d, group: gid }); }
    if (d.to === 'me') { people[d.from] = people[d.from] || { v: 0, items: [] }; people[d.from].v += d.amount; people[d.from].items.push({ ...d, group: gid }); }
  }));
  const list = Object.entries(people).map(([id, p]) => ({ id, ...p, v: r2(p.v) })).sort((a, b) => b.v - a.v);
  const owesMe = list.filter((p) => p.v > 0), iOwe = list.filter((p) => p.v < 0);
  const hist = [...app.settlements].sort((a, b) => b.date.localeCompare(a.date));
  const max = Math.max(...list.map((p) => Math.abs(p.v)), 1);

  const PersonRow = ({ p, i }) => {
    const pos = p.v > 0;
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 10px', margin: '0 4px', borderTop: i ? `1px solid ${T.line}` : 0 }}>
        <Avatar id={p.id} size={40} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 15, fontWeight: 700 }}>{STB_PEOPLE[p.id].name}</div>
          <div style={{ fontSize: 12, color: T.ink3, fontWeight: 500, marginTop: 1 }}>{p.items.map((it) => app.groups.find((g) => g.id === it.group).name).join(', ')}</div>
          <div style={{ marginTop: 6, height: 4, borderRadius: 2, background: T.surface2 }}>
            <div className="grow" style={{ height: '100%', borderRadius: 2, width: `${(Math.abs(p.v) / max) * 100}%`, background: pos ? T.pos : T.neg }} />
          </div>
        </div>
        <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 6 }}>
          <Money v={Math.abs(p.v)} size={15} weight={800} color={pos ? T.pos : T.neg} />
          <button className={pos ? 'btn-sm-tonal' : 'btn-sm-dark'} onClick={() => app.openSettle(p.items[0])}>{pos ? 'Remind' : 'Pay'}</button>
        </div>
      </div>
    );
  };

  return (
    <ScreenBody pb={40}>
      <TopBar onBack={app.back} title="Balances" sub="Across all groups" right={<IconBtn name="tune" />} />
      <div style={{ padding: '4px 16px 0' }}>
        <Card pad={20}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
            <Donut a={tot.owed} b={tot.owe} />
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 12 }}>
              <div>
                <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 600, display: 'flex', alignItems: 'center', gap: 6 }}><span style={{ width: 8, height: 8, borderRadius: 4, background: T.pos }} />Owed to you</div>
                <Money v={tot.owed} size={20} weight={800} color={T.pos} />
              </div>
              <div>
                <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 600, display: 'flex', alignItems: 'center', gap: 6 }}><span style={{ width: 8, height: 8, borderRadius: 4, background: T.neg }} />You owe</div>
                <Money v={tot.owe} size={20} weight={800} color={T.neg} />
              </div>
            </div>
          </div>
        </Card>
      </div>
      {owesMe.length > 0 && <div style={{ padding: '24px 16px 0' }}>
        <SectionTitle>Owes you</SectionTitle>
        <Card pad={6}>{owesMe.map((p, i) => <PersonRow key={p.id} p={p} i={i} />)}</Card>
      </div>}
      {iOwe.length > 0 && <div style={{ padding: '24px 16px 0' }}>
        <SectionTitle>You owe</SectionTitle>
        <Card pad={6}>{iOwe.map((p, i) => <PersonRow key={p.id} p={p} i={i} />)}</Card>
      </div>}
      <div style={{ padding: '24px 16px 0' }}>
        <SectionTitle>Settlement history</SectionTitle>
        <Card pad={6}>{hist.map((s, i) => <SettlementRow key={s.id} s={s} divider={i > 0} app={app} />)}</Card>
      </div>
    </ScreenBody>
  );
}

function Donut({ a, b, size = 112 }) {
  const tot = a + b || 1, r = 46, c = 2 * Math.PI * r, gap = 6;
  const la = (a / tot) * c, lb = (b / tot) * c;
  return (
    <div style={{ position: 'relative', width: size, height: size, flexShrink: 0 }}>
      <svg viewBox="0 0 112 112" width={size} height={size} style={{ transform: 'rotate(-90deg)' }}>
        <circle cx="56" cy="56" r={r} fill="none" stroke={T.surface2} strokeWidth="12" />
        <circle className="dash" cx="56" cy="56" r={r} fill="none" stroke={T.pos} strokeWidth="12" strokeLinecap="round" strokeDasharray={`${Math.max(la - gap, 0)} ${c}`} />
        <circle className="dash" cx="56" cy="56" r={r} fill="none" stroke={T.neg} strokeWidth="12" strokeLinecap="round" strokeDasharray={`${Math.max(lb - gap, 0)} ${c}`} strokeDashoffset={-la} />
      </svg>
      <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ fontSize: 10.5, fontWeight: 700, color: T.ink3, letterSpacing: 0.4, textTransform: 'uppercase' }}>Net</div>
        <div className="num" style={{ fontSize: 15, fontWeight: 800, color: a - b >= 0 ? T.pos : T.neg }}>{stbFmt(a - b, true)}</div>
      </div>
    </div>
  );
}

// Settle sheet
function SettleSheet({ app, d, onClose }) {
  const [amt, setAmt] = React.useState(d.amount.toFixed(2));
  const [method, setMethod] = React.useState('Bank transfer');
  const [done, setDone] = React.useState(false);
  const nm = (id) => id === 'me' ? 'You' : STB_PEOPLE[id].name;
  const g = app.groups.find((x) => x.id === d.group);
  const confirm = () => {
    setDone(true);
    setTimeout(() => { app.settle({ ...d, amount: parseFloat(amt) || d.amount, method }); onClose(); }, 900);
  };
  if (done) return (
    <div style={{ textAlign: 'center', padding: '24px 0 32px' }}>
      <div className="pop" style={{ width: 72, height: 72, borderRadius: 36, background: T.pos, color: '#fff', display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}>
        <Icon name="check" size={40} weight={700} />
      </div>
      <div style={{ fontSize: 20, fontWeight: 800, marginTop: 16 }}>Marked as settled</div>
      <div className="num" style={{ fontSize: 14, color: T.ink3, fontWeight: 600, marginTop: 4 }}>{stbFmt(parseFloat(amt) || 0)} · {method}</div>
    </div>
  );
  return (
    <div>
      <div style={{ fontSize: 20, fontWeight: 800, letterSpacing: -0.3 }}>Settle up</div>
      <div style={{ fontSize: 13, color: T.ink3, fontWeight: 600, marginTop: 2 }}>{g?.name}</div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 16, margin: '24px 0 8px' }}>
        <div style={{ textAlign: 'center' }}><Avatar id={d.from} size={56} /><div style={{ fontSize: 13, fontWeight: 700, marginTop: 6 }}>{nm(d.from).split(' ')[0]}</div></div>
        <div style={{ width: 44, height: 44, borderRadius: 22, background: T.surface, display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: -18 }}><Icon name="arrow_forward" size={22} /></div>
        <div style={{ textAlign: 'center' }}><Avatar id={d.to} size={56} /><div style={{ fontSize: 13, fontWeight: 700, marginTop: 6 }}>{nm(d.to).split(' ')[0]}</div></div>
      </div>
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'baseline', margin: '8px 0 20px' }}>
        <span className="num" style={{ fontSize: 28, fontWeight: 700, color: T.ink3 }}>$</span>
        <input className="num amt-input" value={amt} onChange={(e) => setAmt(e.target.value.replace(/[^0-9.]/g, ''))} style={{ width: `${Math.max(amt.length, 3) * 0.62}em` }} />
      </div>
      <div style={{ fontSize: 12, fontWeight: 700, color: T.ink3, letterSpacing: 0.4, textTransform: 'uppercase', marginBottom: 8 }}>Payment method</div>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 24 }}>
        {[['Bank transfer', 'account_balance'], ['Cash', 'payments'], ['Card', 'credit_card']].map(([m, ic]) => (
          <Chip key={m} icon={ic} active={method === m} onClick={() => setMethod(m)}>{m}</Chip>
        ))}
      </div>
      <button className="btn-primary" onClick={confirm}><Icon name="check_circle" size={22} />Mark as settled</button>
    </div>
  );
}

Object.assign(window, { GroupBalances, BalancesScreen, SettleSheet, SettlementRow, DebtRow, Donut, AllSettled });
