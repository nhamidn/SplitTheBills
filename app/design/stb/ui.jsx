// Split the Bills — design tokens + shared UI atoms

const T = {
  bg: '#F5F4F0',
  surface: '#FFFFFF',
  surface2: '#EDECE7',
  ink: '#1B1E1D',
  ink2: '#5C6260',
  ink3: '#8E9491',
  line: 'rgba(27,30,29,0.08)',
  pos: 'oklch(0.56 0.12 160)',
  posSoft: 'oklch(0.95 0.035 160)',
  posOnDark: 'oklch(0.80 0.13 160)',
  neg: 'oklch(0.63 0.15 33)',
  negSoft: 'oklch(0.95 0.03 33)',
  negOnDark: 'oklch(0.78 0.12 33)',
  font: "'Manrope', system-ui, sans-serif",
  r: 24,
};

function Icon({ name, size = 22, fill = 0, weight = 450, color, style }) {
  return (
    <span className="material-symbols-rounded" style={{
      fontSize: size, color, lineHeight: 1, display: 'inline-block', userSelect: 'none',
      fontVariationSettings: `'FILL' ${fill}, 'wght' ${weight}, 'GRAD' 0, 'opsz' 24`,
      ...style,
    }}>{name}</span>
  );
}

function initials(name) { return name.split(' ').map((s) => s[0]).join('').slice(0, 2); }

function Avatar({ id, size = 36, ring, style }) {
  const p = STB_PEOPLE[id];
  return (
    <div title={p.name} style={{
      width: size, height: size, borderRadius: '50%', flexShrink: 0,
      background: `oklch(0.88 0.06 ${p.hue})`, color: `oklch(0.36 0.09 ${p.hue})`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      fontSize: size * 0.36, fontWeight: 700, letterSpacing: 0.2,
      boxShadow: ring ? `0 0 0 2px ${ring}` : 'none', ...style,
    }}>{initials(p.name)}</div>
  );
}

function AvatarStack({ ids, size = 26, max = 4, ring = T.surface }) {
  const shown = ids.slice(0, max);
  const extra = ids.length - shown.length;
  return (
    <div style={{ display: 'flex', alignItems: 'center' }}>
      {shown.map((id, i) => <Avatar key={id} id={id} size={size} ring={ring} style={{ marginLeft: i ? -size * 0.3 : 0 }} />)}
      {extra > 0 && (
        <div style={{
          width: size, height: size, borderRadius: '50%', marginLeft: -size * 0.3,
          background: T.surface2, color: T.ink2, fontSize: size * 0.36, fontWeight: 700,
          display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: `0 0 0 2px ${ring}`,
        }}>+{extra}</div>
      )}
    </div>
  );
}

function GroupTile({ group, size = 48, radius = 16 }) {
  return (
    <div style={{
      width: size, height: size, borderRadius: radius, flexShrink: 0,
      background: `oklch(0.93 0.045 ${group.hue})`, color: `oklch(0.42 0.1 ${group.hue})`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}><Icon name={group.icon} size={size * 0.5} fill={1} /></div>
  );
}

function CatTile({ cat, size = 44, tone = 'neutral' }) {
  const c = STB_CATS[cat] || STB_CATS.other;
  const bg = tone === 'pos' ? T.posSoft : tone === 'neg' ? T.negSoft : T.surface2;
  const fg = tone === 'pos' ? T.pos : tone === 'neg' ? T.neg : T.ink;
  return (
    <div style={{
      width: size, height: size, borderRadius: 14, flexShrink: 0, background: bg, color: fg,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
    }}><Icon name={c.icon} size={size * 0.48} /></div>
  );
}

function Money({ v, size = 15, weight = 700, sign = false, tone, color }) {
  const c = color || (tone === 'auto' ? (v > 0.004 ? T.pos : v < -0.004 ? T.neg : T.ink3) : T.ink);
  return <span className="num" style={{ fontSize: size, fontWeight: weight, color: c }}>{stbFmt(v, sign)}</span>;
}

function Card({ children, style, onClick, pad = 16 }) {
  return (
    <div className={onClick ? 'press' : ''} onClick={onClick} style={{
      background: T.surface, borderRadius: T.r, padding: pad,
      boxShadow: '0 1px 2px rgba(27,30,29,0.04), 0 4px 16px rgba(27,30,29,0.04)',
      ...style,
    }}>{children}</div>
  );
}

function SectionTitle({ children, action, onAction, style }) {
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', padding: '0 4px', marginBottom: 12, ...style }}>
      <div style={{ fontSize: 17, fontWeight: 700, color: T.ink, letterSpacing: -0.2 }}>{children}</div>
      {action && <button className="txtbtn" onClick={onAction}>{action}</button>}
    </div>
  );
}

function IconBtn({ name, onClick, bg = 'transparent', color = T.ink, size = 44, badge, fill }) {
  return (
    <button className="iconbtn" onClick={onClick} style={{ width: size, height: size, background: bg, color }}>
      <Icon name={name} size={24} fill={fill} />
      {badge && <span style={{ position: 'absolute', top: 10, right: 11, width: 8, height: 8, borderRadius: 4, background: T.neg, boxShadow: `0 0 0 2px ${T.bg}` }} />}
    </button>
  );
}

function TopBar({ title, onBack, right, sub }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '8px 8px 8px 4px', minHeight: 60 }}>
      {onBack && <IconBtn name="arrow_back" onClick={onBack} />}
      <div style={{ flex: 1, minWidth: 0, paddingLeft: onBack ? 4 : 12 }}>
        <div style={{ fontSize: 20, fontWeight: 700, letterSpacing: -0.3, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{title}</div>
        {sub && <div style={{ fontSize: 12.5, color: T.ink3, fontWeight: 500 }}>{sub}</div>}
      </div>
      {right}
    </div>
  );
}

function Chip({ children, active, onClick, icon, style }) {
  return (
    <button onClick={onClick} className="chip" style={{
      background: active ? T.ink : T.surface, color: active ? '#fff' : T.ink,
      borderColor: active ? T.ink : T.line, ...style,
    }}>
      {icon && <Icon name={icon} size={18} />}
      {children}
    </button>
  );
}

function Segmented({ options, value, onChange, style }) {
  const idx = options.findIndex((o) => o.value === value);
  return (
    <div style={{ position: 'relative', display: 'flex', background: T.surface2, borderRadius: 999, padding: 4, ...style }}>
      <div style={{
        position: 'absolute', top: 4, bottom: 4, left: `calc(4px + ${idx} * (100% - 8px) / ${options.length})`,
        width: `calc((100% - 8px) / ${options.length})`, borderRadius: 999, background: T.surface,
        boxShadow: '0 1px 3px rgba(0,0,0,0.08)', transition: 'left .28s cubic-bezier(.3,.7,.2,1)',
      }} />
      {options.map((o) => (
        <button key={o.value} onClick={() => onChange(o.value)} style={{
          flex: 1, position: 'relative', zIndex: 1, height: 38, border: 0, background: 'transparent',
          fontFamily: T.font, fontSize: 14, fontWeight: 700, cursor: 'pointer',
          color: o.value === value ? T.ink : T.ink2, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
        }}>{o.icon && <Icon name={o.icon} size={18} />}{o.label}</button>
      ))}
    </div>
  );
}

function StatusPill({ status }) {
  const done = status === 'completed';
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 4, height: 22, padding: '0 8px', borderRadius: 999,
      fontSize: 11.5, fontWeight: 700, letterSpacing: 0.2,
      background: done ? T.posSoft : 'oklch(0.95 0.05 85)', color: done ? T.pos : 'oklch(0.5 0.1 70)',
    }}>
      <Icon name={done ? 'check_circle' : 'schedule'} size={14} fill={1} />
      {done ? 'Settled' : 'Pending'}
    </span>
  );
}

function BottomNav({ tab, onTab }) {
  const items = [
    { k: 'home', icon: 'home', label: 'Home' },
    { k: 'groups', icon: 'group', label: 'Groups' },
    { k: 'activity', icon: 'receipt_long', label: 'Activity' },
    { k: 'profile', icon: 'person', label: 'Profile' },
  ];
  return (
    <div style={{
      position: 'absolute', left: 0, right: 0, bottom: 0, height: 76, zIndex: 20,
      background: 'rgba(245,244,240,0.92)', backdropFilter: 'blur(12px)',
      borderTop: `1px solid ${T.line}`, display: 'flex', alignItems: 'center',
    }}>
      {items.map((it) => {
        const a = it.k === tab;
        return (
          <button key={it.k} onClick={() => onTab(it.k)} style={{
            flex: 1, border: 0, background: 'transparent', cursor: 'pointer', fontFamily: T.font,
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, padding: '6px 0',
          }}>
            <div style={{
              width: 60, height: 32, borderRadius: 16, display: 'flex', alignItems: 'center', justifyContent: 'center',
              background: a ? T.ink : 'transparent', color: a ? '#fff' : T.ink2, transition: 'background .25s',
            }}><Icon name={it.icon} size={22} fill={a ? 1 : 0} /></div>
            <span style={{ fontSize: 12, fontWeight: a ? 800 : 600, color: a ? T.ink : T.ink2 }}>{it.label}</span>
          </button>
        );
      })}
    </div>
  );
}

function Fab({ onClick, label = 'Add expense', bottom = 92 }) {
  return (
    <button className="fab" onClick={onClick} style={{ bottom }}>
      <Icon name="add" size={24} weight={600} />
      {label}
    </button>
  );
}

function Sheet({ open, onClose, children }) {
  if (!open) return null;
  return (
    <div style={{ position: 'absolute', inset: 0, zIndex: 50 }}>
      <div onClick={onClose} className="scrim" style={{ position: 'absolute', inset: 0, background: 'rgba(20,22,21,0.42)' }} />
      <div className="sheet" style={{
        position: 'absolute', left: 0, right: 0, bottom: 0, background: T.bg,
        borderRadius: '28px 28px 0 0', padding: '10px 20px 24px', maxHeight: '90%', overflow: 'auto',
      }}>
        <div style={{ width: 36, height: 4, borderRadius: 2, background: 'rgba(27,30,29,0.2)', margin: '0 auto 16px' }} />
        {children}
      </div>
    </div>
  );
}

function Snackbar({ msg }) {
  if (!msg) return null;
  return (
    <div key={msg.id} className="snack" style={{
      position: 'absolute', left: 16, right: 16, bottom: 92, zIndex: 60, background: T.ink, color: '#fff',
      borderRadius: 16, padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12, fontSize: 14, fontWeight: 600,
      boxShadow: '0 8px 24px rgba(0,0,0,0.2)',
    }}>
      <Icon name="check_circle" size={20} fill={1} color={T.posOnDark} />{msg.text}
    </div>
  );
}

function Toggle({ on, onChange }) {
  return (
    <button onClick={() => onChange(!on)} aria-pressed={on} style={{
      width: 52, height: 32, borderRadius: 16, border: on ? 0 : `2px solid ${T.ink3}`, cursor: 'pointer',
      background: on ? T.ink : T.surface2, position: 'relative', transition: 'background .2s', flexShrink: 0, boxSizing: 'border-box',
    }}>
      <span style={{
        position: 'absolute', top: '50%', left: on ? 24 : 6, width: on ? 24 : 16, height: on ? 24 : 16, borderRadius: '50%',
        background: on ? '#fff' : T.ink3, transform: 'translateY(-50%)', transition: 'all .2s', display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>{on && <Icon name="check" size={16} color={T.ink} weight={700} />}</span>
    </button>
  );
}

// Proportional bar: owed vs owe
function SplitBar({ a, b, h = 8, gap = 4, ca = T.pos, cb = T.neg, track = 'rgba(255,255,255,0.12)' }) {
  const tot = a + b;
  if (tot < 0.01) return <div style={{ height: h, borderRadius: h, background: track }} />;
  return (
    <div style={{ display: 'flex', gap, height: h }}>
      {a > 0 && <div style={{ flex: a, borderRadius: h, background: ca, transition: 'flex .6s' }} />}
      {b > 0 && <div style={{ flex: b, borderRadius: h, background: cb, transition: 'flex .6s' }} />}
    </div>
  );
}

const ScreenBody = ({ children, pb = 110, style }) => (
  <div className="scroll screen-in" style={{ position: 'absolute', inset: 0, overflowY: 'auto', paddingBottom: pb, ...style }}>{children}</div>
);

Object.assign(window, {
  T, Icon, Avatar, AvatarStack, GroupTile, CatTile, Money, Card, SectionTitle, IconBtn, TopBar, Chip,
  Segmented, StatusPill, BottomNav, Fab, Sheet, Snackbar, Toggle, SplitBar, ScreenBody, initials,
});
